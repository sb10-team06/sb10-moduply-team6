# 콘텐츠 큐레이션 및 실시간 같이보기 플랫폼

[![codecov](https://codecov.io/gh/sb10-team06/sb10-moduply-team6/graph/badge.svg)](https://codecov.io/gh/sb10-team06/sb10-moduply-team6)

## 프로젝트 소개

콘텐츠(영화/드라마/스포츠)를 평가하고 큐레이션하며, 실시간으로 같이 시청하고 채팅할 수 있는 플랫폼입니다.

## 팀원 구성

| 이름 | 역할 | GitHub |
|---|---|---|
| | | |
| | | |
| | | |

## 기술 스택
![Java](https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-009688?style=for-the-badge)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

- **Backend**: Java 17, Spring Boot, Spring Security, Spring Batch
- **Database**: PostgreSQL, H2 (test)
- **실시간 통신**: WebSocket(STOMP), Server-Sent Events
- **External API**: TMDB, The Sports DB
- **Infra**: AWS ECS, Docker Compose
- **CI/CD**: GitHub Actions, Codecov
- **Code Review**: CodeRabbit

## 팀원별 구현 기능

- 

## 실행 방법

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```

## k6 Prometheus/Grafana 성능 측정

API 부하 테스트는 k6 Docker 컨테이너에서 실행하고, Prometheus remote write로 저장한 뒤 Grafana에서 확인한다. 로컬에 k6를 설치하지 않아도 된다.

### 대화방 목록 조회 HikariCP 커넥션 풀 설정

대화방 목록 조회 부하 테스트에서 HikariCP 지표는 Spring Actuator의 `/actuator/prometheus`를 Prometheus가 수집하는 방식으로 확인했다. `max active / max`, `min idle`, `max pending`, `max acquire time` 지표를 기준으로 커넥션 풀 대기 여부를 분석했다.

| HikariCP max pool | max active / max | min idle | max pending | max acquire time | 판단 |
|---:|---:|---:|---:|---:|---|
| 10 | 10 / 10 | 0 | 185 | 14.37 s | 커넥션 풀이 모두 사용되어 대기가 크게 발생함 |
| 30 | 30 / 30 | 0 | 160 | 3.97 s | 대기는 남았지만 acquire 시간이 크게 줄어 가장 안정적임 |
| 50 | 50 / 50 | 0 | 132 | 5.00 s | pending은 줄었지만 DB 동시 처리 부담으로 응답 시간이 악화됨 |

위 결과를 기준으로 local profile의 HikariCP 풀을 아래처럼 설정했다.

```yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
```

### 1. Prometheus와 Grafana 실행

```powershell
docker compose -f docker-compose-k6.yml up -d prometheus grafana
```

접속 주소:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

Grafana 기본 계정은 `admin / admin`이다.

### 2. Grafana 데이터 소스 연결

Grafana에서 `Connections` -> `Data sources` -> `Prometheus`를 추가한다.

```text
URL: http://prometheus:9090
```

Grafana 컨테이너에서 Prometheus 컨테이너를 바라보는 주소이므로 `localhost`가 아니라 `prometheus` 서비스명을 사용한다.

### 3. Spring 서버 실행

#### 콘텐츠 생성 API 부하 테스트용 서버 실행

동기/비동기 측정 차이는 k6 명령이 아니라 Spring Boot 서버의 `MODUPLY_BINARY_CONTENT_ASYNC_ENABLED` 값으로 결정된다. 측정하려는 방식에 맞게 서버를 먼저 실행한다.

부하 테스트 전에 콘텐츠 더미 데이터를 준비하려면 `data-gen` profile을 함께 활성화한다. 기본값은 기존 `k6-seed-` 데이터가 있으면 중복 생성을 건너뛴다.

```powershell
$env:SPRING_PROFILES_ACTIVE="local,data-gen"
$env:MODUPLY_TEST_DATA_CONTENT_ENABLED="true"
$env:MODUPLY_TEST_DATA_CONTENT_TOTAL_SIZE="10000"
$env:MODUPLY_TEST_DATA_CONTENT_CHUNK_SIZE="1000"
.\gradlew.bat bootRun
```

더미 데이터 생성이 끝나면 서버를 종료하고, 아래 동기/비동기 측정용 설정으로 다시 실행한다.

동기 방식 서버 실행:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:MODUPLY_BINARY_CONTENT_ASYNC_ENABLED="false"
.\gradlew.bat bootRun
```

비동기 방식 서버 실행:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
$env:MODUPLY_BINARY_CONTENT_ASYNC_ENABLED="true"
.\gradlew.bat bootRun
```

macOS/Linux:

```bash
SPRING_PROFILES_ACTIVE=local,data-gen MODUPLY_TEST_DATA_CONTENT_ENABLED=true MODUPLY_TEST_DATA_CONTENT_TOTAL_SIZE=10000 ./gradlew bootRun
SPRING_PROFILES_ACTIVE=local MODUPLY_BINARY_CONTENT_ASYNC_ENABLED=false ./gradlew bootRun
SPRING_PROFILES_ACTIVE=local MODUPLY_BINARY_CONTENT_ASYNC_ENABLED=true ./gradlew bootRun
```

기본값은 `true`다.

일반 실행:

```powershell
.\gradlew.bat bootRun
```

### 4. k6 실행

#### 콘텐츠 생성 API k6 실행

`ACCESS_TOKEN`에는 관리자 또는 콘텐츠 생성 권한이 있는 사용자의 Access Token을 넣는다. 토큰 값은 코드나 문서에 커밋하지 않는다.

동기 방식 측정은 서버를 `MODUPLY_BINARY_CONTENT_ASYNC_ENABLED=false`로 실행한 상태에서 수행한다.

```powershell
docker compose -f docker-compose-k6.yml run --rm `
  -e BASE_URL=http://host.docker.internal:8080 `
  -e ACCESS_TOKEN="ACCESS_TOKEN_VALUE" `
  -e VUS=5 `
  -e ITERATIONS=50 `
  k6 run -o experimental-prometheus-rw --tag testid=content-create-sync /scripts/content-create.js
```

비동기 방식 측정은 서버를 `MODUPLY_BINARY_CONTENT_ASYNC_ENABLED=true`로 실행한 상태에서 수행한다.

```powershell
docker compose -f docker-compose-k6.yml run --rm `
  -e BASE_URL=http://host.docker.internal:8080 `
  -e ACCESS_TOKEN="ACCESS_TOKEN_VALUE" `
  -e VUS=5 `
  -e ITERATIONS=50 `
  k6 run -o experimental-prometheus-rw --tag testid=content-create-async /scripts/content-create.js
```

### 5. Grafana에서 확인할 PromQL

```promql
k6_http_req_duration_p95
```

```promql
k6_http_req_duration_avg
```

```promql
k6_http_req_failed_rate
```

```promql
rate(k6_http_reqs_total[1m])
```

동기/비동기 결과는 `testid` 태그로 필터링한다.

```promql
k6_http_req_duration_p95{testid="content-create-sync"}
```

```promql
k6_http_req_duration_p95{testid="content-create-async"}
```

### 6. 종료

```powershell
docker compose -f docker-compose-k6.yml down
```

## API 명세

- **REST API**: [Swagger/OpenAPI 링크 추가 예정](http://example.com/swagger)
- **WebSocket 명세**: [/ws 엔드포인트 및 STOMP 채팅·시청 세션 명세 추가 예정](http://example.com/websocket-docs)
- **SSE 명세**: [/api/sse 엔드포인트 및 알림·DM 명세 추가 예정](http://example.com/sse-docs)

## 배포 링크

- **Backend API Server**: (프로덕션 링크 추가 예정)
- **API 명세 (Swagger UI)**: (프로덕션 배포 시 자동 제공)
