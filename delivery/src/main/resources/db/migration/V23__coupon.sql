-- coupon 모듈: coupon(쿠폰 정의) + issuance(발급 1건)
-- shop_id는 shop 모듈 테이블에 대한 논리 참조라 FK를 걸지 않는다(모듈 간 FK 금지).
-- null이면 특정 상점이 아니라 플랫폼 전체 쿠폰이다.

CREATE TABLE coupon (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    shop_id          BIGINT,
    total_quantity   INT          NOT NULL,
    issued_quantity  INT          NOT NULL DEFAULT 0,
    validity_days    INT          NOT NULL,
    starts_at        DATETIME(6),
    created_at       DATETIME(6)  NOT NULL
);

-- coupon_id는 같은 coupon 모듈 내 참조라 FK를 허용한다(모듈 간 FK만 금지).
CREATE TABLE issuance (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    coupon_id   BIGINT       NOT NULL,
    status      VARCHAR(10)  NOT NULL,
    issued_at   DATETIME(6)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6),

    CONSTRAINT fk_issuance_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id)
);

-- 같은 사용자가 같은 쿠폰을 두 번 발급받지 못하게 하는 최종 안전망.
-- 애플리케이션 로직이 막더라도 동시 요청 경합 시 DB 제약이 마지막 방어선이 된다(커밋 53-7 PAAR).
CREATE UNIQUE INDEX uk_issuance_user_coupon ON issuance (user_id, coupon_id);

-- 만료 배치가 상태별로 대상(ISSUED이면서 유효기간이 지난 건)을 골라내는 조회가 있어 인덱스 필요.
CREATE INDEX idx_issuance_status ON issuance (status);

-- 쿠폰 상세에서 발급 목록을 조회하는 화면(운영자)이 있어 인덱스 필요.
CREATE INDEX idx_issuance_coupon ON issuance (coupon_id);
