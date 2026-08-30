-- shop 모듈: 영업시간(business_hours) + 상점 영업상태(shop.status)

ALTER TABLE shop
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CLOSED' AFTER phone;

CREATE TABLE business_hours (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id       BIGINT      NOT NULL,
    day_of_week   VARCHAR(10) NOT NULL,
    open_time     TIME        NOT NULL,
    close_time    TIME        NOT NULL,
    created_at    DATETIME(6) NOT NULL,
    updated_at    DATETIME(6) NOT NULL,

    CONSTRAINT fk_business_hours_shop FOREIGN KEY (shop_id) REFERENCES shop (id)
);

-- 상점 상세 화면에서 shop_id로 그 상점의 요일별 영업시간 전체를 조회(WHERE shop_id = ?)하는 경로가 있어 필요.
-- day_of_week까지 포함한 복합 인덱스는 만들지 않는다 — 요일 단위로 단건 조회하는 경로가 아직 없어 과설계.
CREATE INDEX idx_business_hours_shop_id ON business_hours (shop_id);
