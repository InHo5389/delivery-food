-- auth 모듈: account, refresh_token, role
-- oauth_link는 아직 도입하지 않음 (커밋 9 시점 제외)

CREATE TABLE account (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    -- 로그인 시 email로 조회(WHERE email = ?)하고, 회원가입 시 이메일 중복 검증에도 쓰인다.
    -- 로그인마다 반드시 타는 경로이므로 유니크 인덱스로 조회 성능과 중복 방지를 동시에 처리한다.
    CONSTRAINT uk_account_email UNIQUE (email)
);

CREATE TABLE refresh_token (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id    BIGINT       NOT NULL,
    token         VARCHAR(512) NOT NULL,
    expires_at    DATETIME(6)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,

    -- /auth/refresh 요청마다 토큰 문자열로 정확히 한 건을 조회(WHERE token = ?)한다.
    -- 등호 조건 단건 조회이므로 유니크 인덱스가 가장 적합하다.
    CONSTRAINT uk_refresh_token_token UNIQUE (token)
);

-- account 탈퇴/재로그인 시 기존 refresh_token을 모두 무효화(WHERE account_id = ?)하는 경로가 있어 필요.
CREATE INDEX idx_refresh_token_account_id ON refresh_token (account_id);
