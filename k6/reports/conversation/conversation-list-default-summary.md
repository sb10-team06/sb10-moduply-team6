# 대화방 목록 조회 부하 테스트 요약

## 테스트 목적

`GET /api/conversations` 대화방 목록 조회 API가 서비스 목표 트래픽에서 어느 수준까지 안정적으로 동작하는지 확인한다.

서비스 전체 목표는 평시 100~300 RPS, 피크 500~1,500 RPS, 최대 2,000+ RPS다. 다만 이 수치는 서비스 전체 API 합산 기준이므로, 이 테스트에서는 대화방 목록 조회 API가 전체 트래픽의 일부를 담당한다고 보고 단일 API 기준 부하를 측정했다.

## 테스트 조건

```powershell
docker compose -f docker-compose-k6.yml run --rm --service-ports `
  -e BASE_URL=http://host.docker.internal:8080 `
  -e USER_COUNT=10000 `
  -e RPS_STAGES=30,60,100,150,200 `
  -e STAGE_DURATION=1m `
  -e K6_KO_HTML_REPORT=/scripts/reports/conversation/conversation-list-default.html `
  k6 run /scripts/conversation-list-default.js
```

| 항목 | 값 |
|---|---:|
| 테스트 사용자 수 | 10,000명 |
| 대상 API | `GET /api/conversations` |
| 요청 방식 | `ramping-arrival-rate` |
| RPS 단계 | 30 -> 60 -> 100 -> 150 -> 200 |
| 단계 유지 시간 | 1분 |
| 조회 limit | 20 |
| 정렬 | `createdAt DESCENDING` |
| 키워드 검색 | 미사용 |

이 조건은 5개 애플리케이션 인스턴스 분산 환경을 가정할 때, 로드밸런서로 유입되는 대화방 목록 조회 API 전체 RPS를 의미한다. 단일 로컬 애플리케이션에서 실행하면 분산 환경보다 더 보수적인 한계 테스트가 된다.

## 테스트 결과

최신 리포트: `conversation-list-default.html`

| 항목 | 전체 요청 기준 | 대화방 목록 API 기준 |
|---|---:|---:|
| 총 요청 수 | 40,299 | 30,299 |
| 초당 요청 수 | 68.35 | 51.39 |
| 평균 응답 시간 | 275.89 ms | 316.56 ms |
| p95 응답 시간 | 1333.30 ms | 1462.44 ms |
| p99 응답 시간 | 1919.36 ms | 1985.23 ms |
| 최대 응답 시간 | 3902.12 ms | 3902.12 ms |
| 성공 트랜잭션 수 | 30,299 | 30,299 |
| 요청 실패율 | 0.00% | 0.00% |
| 체크 성공률 | 100.00% | 100.00% |

요청 실패는 없었지만, 조회 API 기준 p95가 1462.44 ms로 조회 API 목표 기준인 500 ms를 초과했다. 따라서 현재 로컬 단일 인스턴스 환경에서는 200 RPS 단계까지 안정적으로 처리한다고 보기 어렵다.

## 시나리오를 이렇게 잡은 이유

- 서비스 전체 피크 RPS를 단일 API에 그대로 적용하지 않기 위해 30~200 RPS 구간으로 조정했다.
- 로그인 부하는 `setup()`에서 분리하고, 본 테스트에서는 대화방 목록 조회만 측정했다.
- 10,000명의 Access Token을 순환 사용하여 특정 사용자 데이터나 캐시에 부하가 몰리지 않도록 했다.
- `arrival-rate` 기반으로 설정하여 VU 수가 아니라 실제 초당 유입 요청량을 기준으로 측정했다.

## 성능 개선 내용

부하 테스트 과정에서 다음 개선을 적용했다.

| 개선 항목 | 내용 | 기대 효과 |
|---|---|---|
| HikariCP pool 조정 | `maximum-pool-size=30`, `minimum-idle=10` | 커넥션 풀 대기 완화 |
| 대화방 정렬 조건 수정 | `createdAt DESC, id DESC`로 인덱스 정렬 방향과 일치 | 목록 조회 정렬 비용 완화 |
| 검색어 없는 조회 최적화 | 검색어가 없으면 user 조인을 생략 | 불필요한 조인 제거 |
| 최신 DM 조회 개선 | `ROW_NUMBER()` 대신 `DISTINCT ON` 사용 | 대화방별 최신 메시지 조회 비용 완화 |
| 최신 DM 쿼리 수 감소 | 최신 DM id 조회 + 엔티티 조회 2쿼리를 projection 1쿼리로 변경 | 요청당 SQL 수 감소 |
| currentUser 재조회 제거 | 인증 principal의 `UserDto`를 재사용 | 요청당 SQL 1개 감소 |
| 상대 유저 profile fetch | 상대 유저 조회 시 profile image를 함께 조회 | LAZY 추가 쿼리 방지 |
| SQL/debug 로그 비활성화 | 부하 테스트 실행 시 Hibernate SQL/bind 로그 off | 로그 출력 병목 제거 |

## 현재 해석

단일 쿼리 `EXPLAIN ANALYZE` 기준으로는 주요 쿼리들이 warm cache에서 빠르게 동작했다. 병목은 특정 쿼리 하나보다는 요청당 여러 DB 작업이 동시에 발생하면서 커넥션 풀 대기와 DB 동시 처리 한계가 누적되는 쪽으로 보인다.

현재 결과는 단일 로컬 앱 기준 한계 측정에 가깝다. 서비스 목표 규모를 검증하려면 로드밸런서 뒤에 여러 애플리케이션 인스턴스를 두고, 전체 서비스 API 비율을 섞은 별도 service-mix 시나리오가 필요하다.

## 다음 개선 방향

- `pg_stat_statements`로 부하 테스트 전체 누적 DB 시간을 확인한다.
- 인증 필터의 요청당 User DB 조회 제거를 검토한다.
- `totalCount`를 기본 조회에서 항상 계산해야 하는지 검토한다.
- 대화방 목록 조회를 포함한 실제 서비스 API 비율 기반 `service-mix` 테스트를 별도로 작성한다.
- 분산 환경에서는 `BASE_URL`을 로드밸런서 주소로 지정하고 전체 RPS 기준으로 재측정한다.
