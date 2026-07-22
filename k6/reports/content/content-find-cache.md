# 콘텐츠 단건 조회 성능 개선

## 문제

콘텐츠 단건 조회 API(`/api/contents/{contentId}`)는 상세 화면 진입 시 호출되는 핵심 조회 API다. 응답에는 콘텐츠 기본 정보뿐 아니라 썸네일 URL, 태그 목록, 평균 평점, 리뷰 수, 현재 시청자 수가 함께 포함된다.

캐시 적용 전에는 단건 조회 요청마다 콘텐츠 엔티티, 태그 목록, 썸네일 Presigned URL, 현재 시청자 수를 매번 조합했다. 일반 부하에서는 안정적으로 응답했지만, 스트레스 테스트에서 목표 RPS를 올리자 처리량이 약 226 TPS 수준에서 정체되고 응답 시간이 크게 증가했다.

캐시 적용 전 스트레스 테스트 결과는 다음과 같다.

| 항목 | 값 |
|---|---:|
| 총 요청 수 | 86,903 |
| 초당 요청 수 | 226.01 |
| 평균 응답 시간 | 9,301.44 ms |
| p95 응답 시간 | 23,564.99 ms |
| p99 응답 시간 | 25,607.45 ms |
| 최대 응답 시간 | 27,390.20 ms |
| 성공 트랜잭션 수 | 86,903 |
| TPS | 226.01 |
| 요청 실패율 | 0.00% |

테스트 조건은 다음과 같다.

```text
RPS_STAGES=300,400,500,700,1000
USER_COUNT=2000
CONTENT_FIND_MODE=mixed
CONTENT_POOL_SORT_BY=rate
PRE_ALLOCATED_VUS=1000
MAX_VUS=6000
```

## 해결과정

콘텐츠 단건 조회에 Redis `contentDetail` 캐시를 적용했다. Spring Cache self-invocation 문제를 피하기 위해 `ContentService` 내부 private 메서드에 캐시를 붙이지 않고, 별도 `ContentDetailCacheService`를 추가했다.

캐시에는 현재 시청자 수를 제외한 정적 상세 정보를 저장한다.

- 콘텐츠 ID
- 콘텐츠 타입
- 제목
- 설명
- 썸네일 URL
- 태그 목록
- 평균 평점
- 리뷰 수

현재 시청자 수(`watcherCount`)는 실시간성이 중요하므로 캐시에 포함하지 않았다. `ContentService.find()`는 `ContentDetailCacheService.find(contentId)`로 캐시된 상세 정보를 조회한 뒤, `WatchingSessionRepository.countByContentId(contentId)`로 현재 시청자 수만 실시간 조회해 최종 `ContentDto`를 조합한다.

썸네일 URL은 S3 Presigned URL이므로 만료 시간보다 캐시 TTL이 길어지면 만료된 URL을 응답할 수 있다. 기본 Presigned URL 만료 시간이 600초이므로, `contentDetail` 캐시 TTL은 5분으로 설정했다.

```text
CONTENT_DETAIL TTL: 5분
S3 Presigned URL 기본 만료 시간: 10분
```

캐시 무효화는 데이터 변경 지점에 적용했다.

- 콘텐츠 수정 시 `contentDetail:{contentId}` evict
- 콘텐츠 삭제 시 `contentDetail:{contentId}` evict
- 리뷰 통계 갱신 시 `contentDetail:{contentId}` evict

또한 Redis 직렬화 안정성을 위해 캐시 DTO 내부 리스트는 `ArrayList`로 저장되도록 보정했다. `Stream.toList()`나 `List.of()`로 만들어진 불변 리스트 구현체가 Redis 타입 검증에서 거부되는 문제를 방지하기 위한 조치다.

## 결과

동일한 조건으로 캐시 적용 후 스트레스 테스트를 다시 실행했다.

| 항목 | 캐시 적용 전 | 캐시 적용 후 |
|---|---:|---:|
| 총 요청 수 | 86,903 | 160,084 |
| 초당 요청 수 | 226.01 | 408.65 |
| 평균 응답 시간 | 9,301.44 ms | 330.55 ms |
| p95 응답 시간 | 23,564.99 ms | 1,608.03 ms |
| p99 응답 시간 | 25,607.45 ms | 2,994.14 ms |
| 최대 응답 시간 | 27,390.20 ms | 3,923.31 ms |
| 성공 트랜잭션 수 | 86,903 | 160,032 |
| TPS | 226.01 | 408.65 |
| 요청 실패율 | 0.00% | 0.03% |

개선 효과는 다음과 같다.

| 지표 | 개선 효과 |
|---|---:|
| 평균 응답 시간 | 약 96.4% 감소 |
| p95 응답 시간 | 약 93.2% 감소 |
| p99 응답 시간 | 약 88.3% 감소 |
| 최대 응답 시간 | 약 85.7% 감소 |
| TPS | 약 80.8% 증가 |
| 총 처리 요청 수 | 약 84.2% 증가 |

캐시 적용 후 콘텐츠 단건 조회는 5대 분산 환경에서 인스턴스 1대가 검증해야 하는 375~400 RPS 수준에 도달했다. 다만 `p95 < 500ms` 기준은 아직 충족하지 못했고, 고부하 후반부에 일부 `dial: i/o timeout`이 발생해 실패율이 0.03%로 측정됐다.

따라서 단건 상세 캐시는 적용 효과가 크지만, 500 RPS 이상 안정 처리를 위해서는 현재 시청자 수 Redis 조회, Tomcat thread, Redis connection pool, k6 부하 발생기 병목 여부를 추가로 점검해야 한다.
