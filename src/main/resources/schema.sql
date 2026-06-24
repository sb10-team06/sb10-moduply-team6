-- 파일 메타데이터 관리 테이블
-- 실제 파일 데이터는 S3에 저장하고 DB에는 파일 정보만 저장
CREATE TABLE binary_contents (
    id              UUID             PRIMARY KEY,
    file_name       VARCHAR(100)     NOT NULL,
    size            BIGINT           NOT NULL,
    content_type    VARCHAR(100)     NOT NULL,
    storage_key     TEXT             NOT NULL,
    status          VARCHAR(30)      NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 같은 S3 파일 경로 중복 저장 방지
    CONSTRAINT uk_binary_contents_storage_key       UNIQUE (storage_key),
    -- 파일 크기는 0초과만 허용
    CONSTRAINT chk_binary_contents_size             CHECK (size > 0),
    CONSTRAINT chk_binary_contents_status           CHECK (status IN ('PROCESSING', 'SUCCESS', 'FAIL'))
);

-- 콘텐츠 관리 테이블 (contents)
CREATE TABLE contents (
    id              UUID             PRIMARY KEY,
    content_img_id  UUID,
    external_api_id VARCHAR(100),
    type            VARCHAR(30)      NOT NULL,
    title           VARCHAR(255)     NOT NULL,
    description     TEXT             NOT NULL,
    average_rating  NUMERIC(3,2)     NOT NULL DEFAULT 0.00,
    review_count    INT              NOT NULL DEFAULT 0,
    watcher_count   BIGINT           NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_contents_content_img
        FOREIGN KEY (content_img_id)
        REFERENCES binary_contents(id),

    CONSTRAINT chk_contents_type
        CHECK (type IN ('movie', 'tvSeries', 'sport'))
);

CREATE INDEX idx_contents_title ON contents (title);
CREATE INDEX idx_contents_type ON contents (type);

-- 사용자 관리 테이블 (users)
CREATE TABLE users (
    id              UUID             PRIMARY KEY,
    profile_img_id  UUID,
    email           VARCHAR(100)     NOT NULL,
    password        VARCHAR(255),
    name            VARCHAR(50)      NOT NULL,
    role            VARCHAR(20)      NOT NULL DEFAULT 'USER',
    is_blocked      BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_email                         UNIQUE(email),
    -- binary_contents_id를 외래키로받는다.
    CONSTRAINT fk_users_profile_img
        FOREIGN KEY (profile_img_id)
        REFERENCES binary_contents(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_users_role                         CHECK (role IN ('USER', 'ADMIN'))
);

-- 소셜 로그인 관리 테이블 (social_accounts)
CREATE TABLE social_accounts (
    id              UUID             PRIMARY KEY,
    user_id         UUID             NOT NULL,
    provider        VARCHAR(30)      NOT NULL,
    provider_id     VARCHAR(255)     NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_social_accounts_user_id
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_social_accounts_provider_identity  UNIQUE (provider, provider_id),
    CONSTRAINT chk_social_accounts_provider          CHECK (provider IN ('KAKAO', 'GOOGLE', 'NAVER'))
);

-- 플레이리스트 테이블
CREATE TABLE playlists (
    id              UUID             PRIMARY KEY,
    owner_id        UUID             NOT NULL,
    title           VARCHAR(100)     NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK 설정: 유저 삭제 시 플레이리스트 자동 삭제
    CONSTRAINT fk_playlist_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- 플레이리스트 구독 테이블
CREATE TABLE playlist_subscriptions (
    id              UUID             PRIMARY KEY,
    playlist_id     UUID             NOT NULL,
    subscriber_id   UUID             NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK 설정: 플레이리스트나 유저 삭제 시 구독 데이터 자동 삭제
    CONSTRAINT fk_playlist_subscriptions_playlist
        FOREIGN KEY (playlist_id)
        REFERENCES playlists(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_playlist_subscriptions_subscriber
        FOREIGN KEY (subscriber_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    -- 동일 플레이리스트 중복 구독 방지
    CONSTRAINT uq_playlist_subscriptions             UNIQUE (playlist_id, subscriber_id)
);

-- 리뷰 테이블
CREATE TABLE reviews (
    id              UUID             PRIMARY KEY DEFAULT gen_random_uuid(),
    content_id      UUID             NOT NULL,
    author_id       UUID             NOT NULL,
    text            TEXT             NOT NULL,
    rating          DOUBLE PRECISION NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK 설정: 콘텐츠나 유저 삭제 시 리뷰 중복 방지
    CONSTRAINT fk_reviews_content
        FOREIGN KEY (content_id)
        REFERENCES contents(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reviews_author
        FOREIGN KEY (author_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    -- 동일한 콘텐츠에 같은 사용자 리뷰 중복 방지
    CONSTRAINT uq_reviews_content_author             UNIQUE (content_id, author_id),
    -- 평점 범위 검증 (0.0 ~ 5.0)
    CONSTRAINT chk_reviews_rating                    CHECK (rating >= 0.0 AND rating <= 5.0)
);

-- 플레이리스트 콘텐츠 테이블
CREATE TABLE playlist_contents (
    id              UUID             PRIMARY KEY,
    playlist_id     UUID             NOT NULL,
    content_id      UUID             NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK 설정: 플레이리스트나 콘텐츠 삭제 시 연결 데이터 자동 삭제
    CONSTRAINT fk_playlist_contents_playlist
        FOREIGN KEY (playlist_id)
        REFERENCES playlists(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_playlist_contents_content
        FOREIGN KEY (content_id)
        REFERENCES contents(id)
        ON DELETE CASCADE,

    -- 동일한 플레이리스트에 같은 콘텐츠 중복 추가 방지
    CONSTRAINT uq_playlist_contents                  UNIQUE (playlist_id, content_id)
);

-- 콘텐츠 태그 테이블 (tags)
CREATE TABLE tags (
    id              UUID             PRIMARY KEY,
    tag_name        VARCHAR(100)     NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 동일한 태그 이름 중복 생성 방지
    CONSTRAINT uk_tags_name                          UNIQUE (tag_name)
);

-- 알림 테이블
CREATE TABLE notifications (
    id              UUID             PRIMARY KEY,
    receiver_id     UUID             NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type            VARCHAR(30)      NOT NULL,
    title           VARCHAR(100)     NOT NULL,
    content         VARCHAR(500)     NOT NULL,
    level           VARCHAR(30)      NOT NULL DEFAULT 'INFO',
    is_read         BOOLEAN          NOT NULL DEFAULT false,

    -- FK 설정: 유저 삭제 시 알림 데이터 자동 삭제
    CONSTRAINT fk_notifications_receiver
        FOREIGN KEY (receiver_id)
        REFERENCES users(id)
        ON DELETE CASCADE,
    -- 알림 유형 값 검증
    CONSTRAINT chk_notifications_type
        CHECK (type IN ('ROLE_CHANGED', 'PLAYLIST_SUBSCRIBED', 'CONTENT_ADDED', 'FOLLOWED', 'FOLLOW_ACTIVITY', 'DM_RECEIVED')),
    -- 알림 수준 값 검증
    CONSTRAINT chk_notifications_level               CHECK (level IN ('INFO', 'WARNING', 'ERROR'))
);

-- 콘텐츠_태그 매핑 테이블 (N:M)
CREATE TABLE content_tags (
    id              UUID             PRIMARY KEY,
    content_id      UUID             NOT NULL,
    tag_id          UUID             NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- FK 설정: 콘텐츠나 태그 삭제 시 연결 데이터 자동 삭제
    CONSTRAINT fk_content_tags_content
        FOREIGN KEY (content_id)
        REFERENCES contents(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_content_tags_tag
        FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE CASCADE,

    -- 동일 콘텐츠에 같은 태그 중복 등록 방지
    CONSTRAINT uk_content_tags_content_tag           UNIQUE (content_id, tag_id)
);

-- 태그별 콘텐츠 조회 성능을 위한 인덱스
CREATE INDEX idx_content_tags_tag_id ON content_tags (tag_id);

-- 사용자 팔로우 관계관리 테이블
CREATE TABLE follows (
    id              UUID             PRIMARY KEY,
    -- 팔로우를 요청한 사용자
    follower_id     UUID             NOT NULL,
    -- 팔로우 대상 사용자
    followee_id     UUID             NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 팔로우 요청자 FK
    -- 사용자 삭제 시 팔로우 관계도 삭제
    CONSTRAINT fk_follows_follower
        FOREIGN KEY (follower_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- 팔로우 대상자 FK
    CONSTRAINT fk_follows_followee
        FOREIGN KEY (followee_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- 같은 사용자 중복 팔로우 방지
    -- ex) A -> B 팔로우 여러 번 불가
    CONSTRAINT uk_follows_follower_followee          UNIQUE (follower_id, followee_id),

    -- 자기 자신 팔로우 방지
    -- ex) A -> A 불가
    CONSTRAINT chk_follows_not_self                  CHECK (follower_id <> followee_id)
);

CREATE TABLE conversations (
    id              UUID             PRIMARY KEY,
    user1_id        UUID             NOT NULL,
    user2_id        UUID             NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 첫 번째 참여 사용자 FK
    CONSTRAINT fk_conversations_user1
        FOREIGN KEY (user1_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- 두 번째 참여 사용자 FK
    CONSTRAINT fk_conversations_user2
        FOREIGN KEY (user2_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    -- 같은 사용자 조합의 대화방 중복 생성 방지
    -- 주의: A-B와 B-A는 다른 값이므로 저장 순서 고정 필요
    CONSTRAINT uk_conversations_user_pair            UNIQUE (user1_id, user2_id),

    -- 자기 자신과 대화방 생성 방지
    CONSTRAINT chk_conversations_different_users     CHECK (user1_id <> user2_id)
);

CREATE TABLE direct_messages (
    id              UUID             PRIMARY KEY,
    conversation_id UUID             NOT NULL,
    sender_id       UUID             NOT NULL,
    content         TEXT,
    -- 메시지 읽음 여부
    is_read         BOOLEAN          NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 대화방 삭제 시 메시지도 삭제
    CONSTRAINT fk_direct_messages_conversation
        FOREIGN KEY (conversation_id)
        REFERENCES conversations(id)
        ON DELETE CASCADE,

    -- 송신자 FK
    CONSTRAINT fk_direct_messages_sender
        FOREIGN KEY (sender_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);
