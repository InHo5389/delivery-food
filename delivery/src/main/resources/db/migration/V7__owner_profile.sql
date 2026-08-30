-- shop 모듈: 사장님 부가정보(사업자정보, 정산계좌)
-- account_id는 auth 모듈의 account.id를 논리 참조한다 (모듈 간 FK 금지 원칙).

CREATE TABLE owner_profile (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id              BIGINT       NOT NULL,
    business_registration_number VARCHAR(20) NOT NULL,
    business_name           VARCHAR(100) NOT NULL,
    settlement_bank         VARCHAR(50)  NOT NULL,
    settlement_account      VARCHAR(50)  NOT NULL,
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,

    -- 사장님 한 명(account)당 프로필은 하나. 로그인 시점마다 account_id로 자기 프로필을 조회하므로
    -- 유니크 인덱스로 조회 성능과 중복 생성 방지를 동시에 처리한다.
    CONSTRAINT uk_owner_profile_account_id UNIQUE (account_id)
);
