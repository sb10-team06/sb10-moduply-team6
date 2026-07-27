# S3 이미지 URL 저장 및 Presigner 제거

- 작성일: 2026-07-26
- 상태: Draft
- 작성 주체: Planner
- 대상 프로젝트: 모두의 플리
- 대체 대상: `docs/plans/plan-2026-07-26-image-url-presigner-performance.md`

## 1. 목표

S3에 이미지 업로드가 완료되면 해당 객체의 일반 URL을 `users` 또는 `contents` 테이블에 저장한다.
이미지 조회에서는 DB에 저장된 URL을 그대로 반환하고 S3 Presigned URL을 더 이상 생성하지 않는다.

- `users.profile_image_url` 컬럼에 프로필 이미지 URL을 저장한다.
- `contents.thumbnail_url` 컬럼에 콘텐츠 이미지 URL을 저장한다.
- 기존 API 응답 필드 `profileImageUrl`, `thumbnailUrl`은 유지한다.
- 기존 `BinaryContent`와 `profile_img_id`, `content_img_id` 관계는 업로드 상태와 기존 파일 삭제를 위해 유지한다.
- `S3Presigner`와 Presigned URL 생성 로직을 제거한다.
- Redis 이미지 URL 캐시는 유지하되 DB URL을 캐시하도록 변경한다.
- S3 업로드와 삭제를 위한 AWS SDK 의존성은 유지한다.

## 2. 배경 및 문제

현재 조회 흐름은 다음과 같다.

`User/Content 조회 → BinaryContentService.generateUrl() → Redis → S3Presigner`

이 방식은 조회할 때마다 URL 캐시를 확인하고, cache miss이면 만료되는 Presigned URL을 생성한다.
또한 사용자, 콘텐츠, 인증, 대화, DM, 플레이리스트, 리뷰 서비스가 URL 생성 로직에 결합되어 있다.

변경 후 흐름은 다음과 같다.

`S3 upload → 일반 S3 object URL 반환 → User/Content URL 컬럼 저장`

`이미지 조회 → Redis imageUrl cache → cache miss 시 DB URL 반환 및 캐시 저장`

## 3. 현재 구조와 변경 구조

| 구분 | 현재 | 변경 후 |
|---|---|---|
| 사용자 이미지 저장 | `profile_img_id`만 저장 | `profile_img_id`와 `profile_image_url` 저장 |
| 콘텐츠 이미지 저장 | `content_img_id`만 저장 | `content_img_id`와 `thumbnail_url` 저장 |
| URL 생성 | 조회 시 Presigner 실행 | upload 성공 시 한 번 생성 |
| 사용자 upload | 동기 upload | upload 반환 URL을 즉시 User에 저장 |
| 콘텐츠 upload | 비동기 event upload | listener가 upload 반환 URL을 Content에 저장 |
| 조회 | `generateUrl()`이 Presigned URL 생성 | Redis에 캐시된 DB URL 반환 |
| cache miss | Presigner 실행 | User/Content의 URL 컬럼 값을 캐시 |
| URL 캐시 | Redis `profileImageUrl` | Redis `imageUrl`, key는 `BinaryContent.id` |

### URL 형식

S3 SDK의 `S3Utilities.getUrl(GetUrlRequest)` 또는 아래 형식으로 일반 object URL을 만든다.

`https://{bucket}.s3.{region}.amazonaws.com/{storageKey}`

Presigned query parameter는 포함하지 않는다.

이 URL로 이미지를 조회하려면 S3 객체에 GET 접근 권한이 있어야 한다.
이번 계획에서는 이미지가 공개 조회 가능한 자산이라는 전제를 사용한다.

## 4. 변경 범위

### 4.1 Entity 및 DB

- `src/main/java/com/team6/moduply/user/entity/User.java`
  - `profileImageUrl` 문자열 필드 추가
  - `updateProfileImage(BinaryContent profileImg, String profileImageUrl)` 형태로 메타데이터와 URL을 함께 변경
- `src/main/java/com/team6/moduply/content/entity/Content.java`
  - `thumbnailUrl` 문자열 필드 추가
  - `updateContentImage(BinaryContent contentImg, String thumbnailUrl)` 형태로 메타데이터와 URL을 함께 변경
- `src/main/resources/schema.sql`
  - `users.profile_image_url TEXT`
  - `contents.thumbnail_url TEXT`
- 기존 운영 DB에는 배포 전에 `ALTER TABLE`을 수동 실행한다.
- 수동 실행 SQL과 실행 여부는 Pull Request 본문에 기록한다.

PR에 기록할 운영 DB 반영 명령:

```sql
\set ON_ERROR_STOP on

BEGIN;
SET LOCAL lock_timeout = '5s';

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_image_url TEXT;

ALTER TABLE contents
    ADD COLUMN IF NOT EXISTS thumbnail_url TEXT;

COMMIT;
```

락 타임아웃이 발생하면 트랜잭션 전체가 롤백된다. 저부하 시간에 장기 트랜잭션을
확인한 뒤 같은 명령을 재실행하며, 락 대기 상태로 둔 채 서비스 요청을 막지 않는다.

운영 환경은 Hibernate `ddl-auto=validate`를 사용하므로 새 애플리케이션 배포 전에 컬럼을 추가한다.
- 테스트 데이터 생성기
  - `UserGenerator.java`
  - `ContentGenerator.java`

### 4.2 S3 및 BinaryContent

- `src/main/java/com/team6/moduply/binarycontent/storage/BinaryContentStorage.java`
  - `generateUrl()` 제거
  - `upload()`이 storage key가 아니라 조회 가능한 object URL을 반환하도록 계약 변경
- `src/main/java/com/team6/moduply/binarycontent/s3/S3BinaryContentStorage.java`
  - `S3Presigner` 필드와 `generateUrl()` 제거
  - `putObject()` 성공 후 일반 S3 object URL 반환
- `src/main/java/com/team6/moduply/binarycontent/storage/local/LocalBinaryContentStorage.java`
  - `generateUrl()` 제거
  - `upload()`이 기존 local URL을 반환하도록 변경
- `src/main/java/com/team6/moduply/binarycontent/s3/S3Config.java`
  - `S3Presigner` Bean 제거
  - `S3Client` Bean 유지
- `src/main/java/com/team6/moduply/binarycontent/s3/S3Properties.java`
  - `presignedUrlExpiration` 제거
- `src/main/java/com/team6/moduply/binarycontent/service/BinaryContentService.java`
  - Presigner 기반 `generateUrl()` 제거
  - 사용자 동기 upload 결과에 URL을 포함해 반환
- 신규 `src/main/java/com/team6/moduply/binarycontent/service/ImageUrlCacheService.java`
  - DB에 저장된 URL을 Redis에 캐시
  - cache key는 `BinaryContent.id`
  - URL을 새로 생성하거나 S3를 호출하지 않음
- `src/main/java/com/team6/moduply/binarycontent/event/BinaryContentStorageEventListener.java`
  - 콘텐츠 upload 성공 시 반환 URL을 Content에 반영
- `src/main/resources/application.yml`
  - `presigned-url-expiration` 제거
- `.env.example`
  - `AWS_S3_PRESIGNED_URL_EXPIRATION` 제거

`software.amazon.awssdk:s3`와 `apache-client`는 upload/delete에 필요하므로 `build.gradle`에서 유지한다.
Presigner는 별도 Gradle 의존성이 아니므로 import, Bean, 필드와 사용 코드만 제거한다.

### 4.3 User 로직

- `UserService.updateUser()`
  - `BinaryContentService.createUserProfile()`의 upload 결과에서 `BinaryContent`와 URL을 받는다.
  - `user.profileImg`와 `user.profileImageUrl`을 함께 갱신한다.
  - 이미지가 없는 수정 요청은 기존 URL을 유지한다.
- `UserService.getUser()`, `findAll()`
  - URL 생성 서비스를 호출하지 않고 Entity의 URL을 사용한다.
- `UserMapper`
  - `User.profileImageUrl`을 `UserDto.profileImageUrl`에 매핑한다.
- 기존 파일 삭제 이벤트 흐름은 유지한다.

### 4.4 Content 로직

- `ContentService.create()`, `update()`
  - 기존 비동기 upload 구조는 유지한다.
  - upload 성공 listener가 반환 URL을 `Content.thumbnailUrl`에 저장한다.
  - upload가 완료되기 전 응답은 기존 URL 또는 `/placeholder-movie.png`을 사용한다.
  - upload 실패 시 새 URL을 저장하지 않고 기존 URL을 유지한다.
- `ContentService.find()`, `findAll()`
  - Entity 또는 cache DTO의 `thumbnailUrl`을 그대로 사용한다.
- `ContentListCacheService`, `ContentDetailCacheService`
  - Presigned URL 대신 DB URL을 캐시에 저장한다.
  - upload 성공 후 콘텐츠 목록·상세 캐시를 제거한다.
- `ExternalContentService`
  - 외부 이미지 다운로드 후 S3 upload가 성공하면 동일하게 `contents.thumbnail_url`을 갱신한다.
- `ContentMapper`
  - `Content.thumbnailUrl`을 `ContentDto.thumbnailUrl`에 매핑한다.

### 4.5 다른 이미지 조회 소비자

다음 코드에서 `BinaryContentService.generateUrl()` 호출을 제거하고
`ImageUrlCacheService`를 통해 User 또는 Content의 DB URL을 조회한다.

- `AuthService`
- `ModuPlyUserDetailsService`
- `ConversationService`
- `ConversationMapper`
- `DirectMessageService`
- `PlaylistService`
- `ReviewService`

외부 DTO 필드명은 변경하지 않는다.

### 4.6 Redis 이미지 URL 캐시 전환

- `CacheConfig.PROFILE_IMAGE_URL`을 `IMAGE_URL`로 변경
- 기존 `profileImageUrl` 캐시와 다른 이름 또는 cache prefix를 사용해 Presigned URL 값과 분리
- `ImageUrlCacheService.find(BinaryContent, String storedUrl)`에 `@Cacheable` 적용
  - key: `BinaryContent.id`
  - value: `User.profileImageUrl` 또는 `Content.thumbnailUrl`
  - cache miss: 호출자가 Entity에서 읽은 DB URL 반환 후 Redis 저장
  - cache hit: Redis의 DB URL 반환
- 프로필·콘텐츠 이미지 교체 또는 삭제 시 이전 `BinaryContent.id`의 `IMAGE_URL` 캐시 제거
- 콘텐츠 이미지 upload 성공 시 신규 URL을 DB에 저장하고 콘텐츠 목록·상세·플레이리스트 상세 캐시 무효화
- DB URL이 null인 경우에는 이미지 URL 캐시에 저장하지 않음

### 4.7 S3 접근 정책

이미지를 일반 S3 URL로 조회할 수 있도록 다음 운영 설정이 필요하다.

- 이미지 object prefix에 `s3:GetObject` 허용
- S3 CORS allowed origin:
  - `https://moduply.co.kr`
- 허용 method:
  - `GET`
  - `HEAD`
- upload와 delete는 애플리케이션 IAM 권한으로만 수행
- bucket write/list 권한은 공개하지 않음

CORS는 브라우저 교차 출처 정책이며 URL을 통한 직접 접근 자체를 차단하지 않는다.
이미지가 비공개 자산이어야 한다면 일반 S3 URL 저장 방식은 사용할 수 없고 Presigner 또는 CDN 인증 방식이 필요하다.

저장소에 S3 bucket policy IaC가 없으므로 실제 정책 적용은 AWS 운영 설정에서 수행하고,
이 저장소에는 정책 예시 또는 운영 문서만 추가한다.

### 4.8 변경하지 않는 범위

- Flyway/Liquibase 도입
- CloudFront, Route 53, 별도 이미지 도메인 도입
- pending 이미지 테이블 또는 신규 생명주기 서비스 도입
- User/Content API 경로와 JSON 필드명 변경
- 이미지 크기와 MIME Type 검증 변경
- `BinaryContent` 테이블 및 기존 이미지 FK 제거

## 5. 구현 계획

### 1단계. URL 컬럼과 마이그레이션 SQL 추가

- 작업 내용:
  - `users.profile_image_url`, `contents.thumbnail_url` nullable 컬럼 추가
  - 신규 DB용 `schema.sql` 수정
  - 운영 기존 DB에 적용할 수동 `ALTER TABLE` 명령을 PR 본문에 기록
- 대상 파일:
  - `schema.sql`
  - `User.java`
  - `Content.java`
- 변경 이유: 이미지 URL을 직접 저장하기 위함
- 선행 조건: S3 bucket과 region 값 확인
- 검증 방법: PostgreSQL에 SQL 적용 후 Hibernate validate와 기존 데이터 조회
- 완료 조건: 신규 DB 스키마 반영과 운영 DB 수동 실행 명령 준비

### 2단계. S3 upload 반환값을 일반 URL로 변경

- 작업 내용:
  - `BinaryContentStorage.generateUrl()` 삭제
  - S3 upload 성공 후 `S3Utilities.getUrl()`로 object URL 반환
  - local upload도 local 조회 URL 반환
- 대상 파일:
  - `BinaryContentStorage.java`
  - `S3BinaryContentStorage.java`
  - `LocalBinaryContentStorage.java`
  - 관련 storage 테스트
- 변경 이유: URL을 조회 시점이 아니라 upload 시점에 한 번만 결정하기 위함
- 선행 조건: URL 형식 확정
- 검증 방법: upload 요청의 bucket/key와 반환 URL 검증
- 완료 조건: Presigner 없이 upload 결과에서 URL 획득

### 3단계. 사용자 이미지 URL 저장

- 작업 내용:
  - 프로필 동기 upload 결과에 `BinaryContent`와 URL을 포함
  - User의 이미지 FK와 URL을 함께 저장
  - 조회 시 DB URL을 Redis 이미지 URL 캐시에 저장하고 재사용
- 대상 파일:
  - `BinaryContentService.java`
  - `User.java`
  - `UserService.java`
  - `UserMapper.java`
- 변경 이유: 프로필 upload 직후 URL을 DB에 저장하기 위함
- 선행 조건: 1~2단계 완료
- 검증 방법: upload 성공, upload 실패, 이미지 없는 수정, 기존 이미지 교체 테스트
- 완료 조건: User 조회에서 Presigner 호출 없이 Redis 또는 DB에 저장된 URL 반환

### 4단계. 콘텐츠 이미지 URL 저장

- 작업 내용:
  - 비동기 listener가 S3 upload 반환 URL을 받음
  - upload 성공 시 `contents.thumbnail_url` 갱신
  - 실패 시 기존 URL 유지
  - 조회 시 DB URL을 Redis 이미지 URL 캐시에 저장하고 재사용
  - 성공 후 콘텐츠 관련 캐시 무효화
- 대상 파일:
  - `BinaryContentStorageEventListener.java`
  - `BinaryContentService.java`
  - `Content.java`
  - `ContentService.java`
  - `ExternalContentService.java`
  - 콘텐츠 cache service
- 변경 이유: 기존 비동기 upload 구조를 유지하면서 URL을 DB에 저장하기 위함
- 선행 조건: 1~2단계 완료
- 검증 방법: 생성·수정·외부 import upload 성공과 실패, cache eviction
- 완료 조건: upload 성공 후 Content 조회가 DB URL 반환

### 5단계. 모든 조회 로직 전환

- 작업 내용:
  - User/Content 관련 서비스와 Mapper의 `generateUrl()` 호출 제거
  - `ImageUrlCacheService`가 Redis cache hit이면 캐시 URL을 반환
  - cache miss이면 Entity의 DB URL을 반환하면서 Redis에 저장
  - 조회된 URL을 기존 DTO 필드에 전달
- 대상 파일:
  - Auth, UserDetails, Conversation, DM, Playlist, Review
  - Content list/detail cache
- 변경 이유: Presigned URL 조회 흐름을 완전히 제거하기 위함
- 선행 조건: User/Content URL 저장 및 `ImageUrlCacheService` 준비
- 검증 방법: cache hit/miss, null URL, 이미지 변경 후 eviction 및 각 응답 DTO 테스트
- 완료 조건: 조회 시 S3 호출 없이 Redis 또는 DB URL 반환

### 6단계. 기존 이미지 데이터 backfill

- 작업 내용:
  - `profile_img_id`, `content_img_id`가 가리키는 `binary_contents.storage_key`로 일반 S3 URL 생성
  - `BinaryContent.status='SUCCESS'`인 행만 URL 컬럼 갱신
  - 이미지가 없거나 FAIL/DELETED인 행은 null 유지
- 기록 위치:
  - Pull Request 본문의 기타 참고 사항 또는 배포 참고 사항
- 변경 이유: 기존 사용자와 콘텐츠도 저장 URL을 사용하기 위함
- 선행 조건: 실제 bucket, region 확정
- 검증 방법:
  - SUCCESS 이미지 중 URL null 0건
  - URL path와 storageKey 일치
  - 샘플 URL GET 성공
- 완료 조건: 기존 정상 이미지 URL backfill 완료

애플리케이션에서 자동 실행하지 않고 운영자가 아래 SQL의 `<bucket>`, `<region>`을
실제 값으로 치환하여 수동 실행한다. 이 SQL은 Pull Request의 `기타 참고 사항`에도
동일하게 기록한다.

```sql
UPDATE users AS u
SET profile_image_url = format(
    'https://%s.s3.%s.amazonaws.com/%s',
    '<bucket>',
    '<region>',
    bc.storage_key
)
FROM binary_contents AS bc
WHERE u.profile_img_id = bc.id
  AND bc.status = 'SUCCESS'
  AND u.profile_image_url IS NULL;

UPDATE contents AS c
SET thumbnail_url = format(
    'https://%s.s3.%s.amazonaws.com/%s',
    '<bucket>',
    '<region>',
    bc.storage_key
)
FROM binary_contents AS bc
WHERE c.content_img_id = bc.id
  AND bc.status = 'SUCCESS'
  AND c.thumbnail_url IS NULL;
```

적용 전에는 두 `UPDATE`의 `WHERE` 조건을 동일하게 사용한 `SELECT`로 대상 건수와
생성될 URL을 확인하고 DB snapshot을 생성한다.

운영 반영 순서는 다음과 같이 고정한다.

1. 프로필·콘텐츠 이미지 신규 등록과 교체 요청을 일시 중지한다.
2. URL 컬럼 `ALTER TABLE`을 실행한다.
3. 이미지 prefix 공개 GET과 `https://moduply.co.kr` CORS 정책을 적용한다.
4. 위 backfill SQL을 실행한다.
5. SUCCESS 이미지 FK 중 URL이 null인 행이 0건인지 검증하고 샘플 GET을 확인한다.
6. 애플리케이션을 배포한다. Redis `v4` prefix가 과거 URL 캐시를 격리한다.
7. 이미지 쓰기를 재개한다.

backfill 검증 전에 URL 컬럼만 읽는 새 애플리케이션을 먼저 배포하면 기존 이미지가
null 또는 placeholder로 보일 수 있으므로 배포 순서를 바꾸지 않는다.

### 6-1단계. S3 조회 및 moduply.co.kr CORS 정책

일반 S3 object URL을 브라우저에서 조회하려면 객체 GET 권한이 별도로 필요하다.
직접 S3 URL을 사용할 경우 이미지 전용 prefix에만 `s3:GetObject`를 허용하고,
bucket CORS에는 서비스 origin만 등록한다.

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedOrigins": ["https://moduply.co.kr"],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

Bucket policy의 Resource는 전체 bucket이 아니라 이미지가 저장되는
`users/*`, `contents/*` prefix로 제한한다. CORS는 접근 권한을 부여하지 않으므로,
공개 GET 허용 여부와 Block Public Access 설정은 배포 담당자가 함께 검증해야 한다.
추후 CloudFront를 도입하면 공개 bucket 대신 OAC를 사용하고 DB URL을 배포 도메인으로
backfill한다.

bucket, region 또는 배포 도메인이 바뀔 때는 초기 backfill SQL을 재사용하지 않는다.
초기 SQL은 URL이 null인 행만 대상으로 하기 때문이다. 변경 작업은 기존 prefix까지
조건에 포함해 별도로 실행한다.

```sql
UPDATE users AS u
SET profile_image_url = format(
    'https://%s.s3.%s.amazonaws.com/%s',
    '<new-bucket>', '<new-region>', bc.storage_key
)
FROM binary_contents AS bc
WHERE u.profile_img_id = bc.id
  AND bc.status = 'SUCCESS'
  AND u.profile_image_url LIKE '<old-base-url>/%';

UPDATE contents AS c
SET thumbnail_url = format(
    'https://%s.s3.%s.amazonaws.com/%s',
    '<new-bucket>', '<new-region>', bc.storage_key
)
FROM binary_contents AS bc
WHERE c.content_img_id = bc.id
  AND bc.status = 'SUCCESS'
  AND c.thumbnail_url LIKE '<old-base-url>/%';
```

재작성 후에는 `imageUrl`, `contentList`, `contentDetail`, `playlistDetail`의
`moduply:cache:v4:` 키만 삭제하거나 cache prefix를 새 버전으로 올린다.
Redis 장애로 비동기 eviction이 실패하면 DB/S3 성공 상태는 유지되고 기존 콘텐츠
응답 캐시는 설정된 TTL인 최대 5분 동안 남을 수 있으며, 이 stale window는 허용한다.

### 7단계. Presigner 설정과 테스트 정리

- 작업 내용:
  - `S3Presigner` Bean/import/필드 제거
  - `presigned-url-expiration`과 환경 변수 제거
  - 기존 Presigned URL Redis 캐시를 DB URL 캐시로 교체
  - Presigner 관련 테스트를 upload URL 및 DB URL cache 테스트로 교체
- 대상 파일:
  - `S3Config.java`
  - `S3Properties.java`
  - `application.yml`
  - `.env.example`
  - `CacheConfig.java`
  - `S3ConfigTest.java`
  - `S3BinaryContentStorageTest.java`
  - `BinaryContentServiceTest.java`
  - 신규 `ImageUrlCacheServiceTest.java`
  - `LocalBinaryContentStorageTest.java`
- 변경 이유: 미사용 Presigner 코드와 설정을 남기지 않기 위함
- 선행 조건: 모든 조회 전환 완료
- 검증 방법:
  - `S3Presigner`
  - `presignGetObject`
  - `presigned-url-expiration`
  - `AWS_S3_PRESIGNED_URL_EXPIRATION`
  - Presigner 기반 `generateUrl`
  전역 검색 결과 0건
- 완료 조건: Presigner 관련 코드·설정·테스트가 없고 Redis에는 DB URL만 저장됨

## 6. 테스트 계획

### 단위 테스트

- S3 upload 성공 시 일반 object URL을 반환한다.
- S3 upload 실패 시 기존 S3 예외를 반환한다.
- 프로필 upload 성공 시 User의 FK와 URL이 함께 변경된다.
- 프로필 upload 실패 시 기존 이미지와 URL이 유지된다.
- 이미지 없는 사용자 수정은 기존 URL을 유지한다.
- 콘텐츠 비동기 upload 성공 시 URL이 저장된다.
- 콘텐츠 비동기 upload 실패 시 신규 URL이 저장되지 않는다.
- 콘텐츠 이미지 교체 성공 후 기존 이미지 삭제 이벤트가 발행된다.
- 외부 콘텐츠 이미지 upload 성공 시 URL이 저장된다.
- Mapper와 조회 서비스가 Entity URL을 그대로 DTO에 전달한다.
- Redis cache miss 시 DB URL을 저장한다.
- Redis cache hit 시 DB 조회 결과를 다시 생성하지 않고 캐시 URL을 반환한다.
- 이미지 교체·삭제 시 이전 이미지 URL 캐시를 제거한다.
- null URL은 캐시하지 않는다.
- Presigner는 호출되지 않는다.

### 통합 테스트

- PostgreSQL에 수동 컬럼 추가 명령과 backfill SQL을 적용한다.
- 기존 SUCCESS 이미지 데이터가 올바른 S3 URL로 변환된다.
- null, PROCESSING, FAIL, DELETED 데이터는 정책대로 null을 유지한다.
- 프로필 upload부터 조회까지 저장 URL이 반환된다.
- 프로필 URL의 cache miss/hit 및 이미지 교체 eviction이 동작한다.
- 콘텐츠 비동기 upload 완료 후 DB와 Redis 캐시에 저장 URL이 반영된다.

### 회귀 테스트

- `profileImageUrl`, `thumbnailUrl` JSON 필드명 유지
- 이미지가 없는 콘텐츠의 `/placeholder-movie.png` 유지
- 회원가입, 로그인, token refresh, UserDetails
- 대화, DM, 플레이리스트, 리뷰
- 콘텐츠 생성·수정·삭제 및 외부 import
- 인증·인가, not-found, 잘못된 이미지 파일 예외
- 전체 `./gradlew test`

테스트 메서드명과 `@DisplayName`은 AGENTS.md 규칙을 따른다.

## 7. 위험 요소

| 위험 | 발생 조건 | 영향 | 대응 |
|---|---|---|---|
| S3 GET 권한 없음 | 일반 object URL 저장 후 bucket 비공개 | 이미지 403 | 배포 전 bucket read policy와 샘플 URL 확인 |
| CORS 설정 누락 | 브라우저 교차 출처 요청 | 일부 프론트 기능 실패 | `https://moduply.co.kr` GET/HEAD 허용 |
| 비동기 콘텐츠 upload 중 조회 | upload 완료 전 조회 | 기존 URL 또는 placeholder 반환 | 성공 listener에서만 신규 URL 저장 |
| upload 성공 후 URL DB 갱신 실패 | S3와 DB 처리 경계 | S3 객체는 있으나 URL null | 오류 로그와 재처리 또는 운영 보정 SQL |
| 기존 Presigned URL 캐시 | 배포 후 기존 cache hit | 만료 URL 반환 | `imageUrl` 캐시명 또는 cache prefix로 분리 |
| 이미지 교체 후 cache stale | DB URL만 변경하고 캐시 미삭제 | 이전 이미지 반환 | 이전 `BinaryContent.id` 캐시 eviction |
| URL과 BinaryContent 불일치 | 별도 필드 갱신 누락 | 잘못된 조회 또는 기존 파일 삭제 | Entity 메서드에서 FK와 URL 함께 변경 |
| S3 bucket/region 변경 | DB에 완전한 URL 저장 | 기존 URL 일괄 수정 필요 | 기존 prefix 조건의 URL 재작성 SQL 실행 후 관련 캐시 제거 |

## 8. 롤백 방안

- 신규 URL 컬럼은 nullable로 유지하므로 이전 애플리케이션으로 되돌릴 수 있다.
- 배포 실패 시 기존 `profile_img_id`, `content_img_id`와 Presigner 버전으로 rollback한다.
- URL backfill 전 DB snapshot을 생성한다.
- backfill URL이 잘못되면 URL 컬럼만 null 또는 이전 값으로 되돌리고 BinaryContent 관계는 유지한다.
- Presigner 관련 환경 변수는 rollback 가능 기간 동안 운영 환경에서 바로 삭제하지 않는다.

## 9. 완료 조건

- [ ] `users.profile_image_url`, `contents.thumbnail_url` 컬럼이 추가되었다.
- [ ] S3 upload가 일반 object URL을 반환한다.
- [ ] 프로필 upload 성공 시 User URL이 저장된다.
- [ ] 콘텐츠 upload 성공 시 Content URL이 저장된다.
- [ ] 모든 이미지 조회가 DB URL을 사용한다.
- [ ] Redis 이미지 URL 캐시에는 DB URL만 저장된다.
- [ ] 이미지 교체·삭제 시 기존 URL 캐시가 제거된다.
- [ ] 기존 SUCCESS 이미지 URL backfill이 완료되었다.
- [ ] `S3Presigner`와 Presigner 설정·테스트가 제거되었다.
- [ ] S3 GET 및 `https://moduply.co.kr` CORS 정책이 적용되었다.
- [ ] 성공·실패·권한·not-found 테스트가 통과했다.
- [ ] 전체 테스트가 통과했다.
- [ ] Reviewer 검토가 완료되었다.

## 10. 확인 필요 사항

1. 실제 S3 bucket과 region을 사용한 object URL 형식
2. S3 이미지가 공개 조회 가능한 자산인지
3. bucket policy와 CORS를 적용할 AWS 운영 담당자
4. 운영 DB에서 수동 `ALTER TABLE`과 backfill SQL을 실행할 담당자 및 절차
