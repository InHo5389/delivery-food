-- order 모듈: cart, cart_items (장바구니 도메인, 커밋 35)
-- ⚠️ 의도적 구식 구현 — Phase 3 C-9에서 Redis Hash로 전환 예정.
--   장바구니는 쓰기가 빈번하고 수명이 짧은 데이터라 MySQL보다 Redis가 적합하지만,
--   Phase 1에서는 개선 전 비교 기준선을 남기기 위해 MySQL로 구현한다.
--
-- shop_id, menu_id는 다른 모듈(shop) 테이블에 대한 논리 참조라 FK를 걸지 않는다(1-8절).
-- cart_items.cart_id는 같은 모듈(order) 내 참조이므로 FK를 허용한다.

CREATE TABLE cart (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT      NOT NULL,
    shop_id     BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,

    -- 고객 1명은 동시에 하나의 장바구니만 가진다(다른 상점 메뉴를 섞어 담을 수 없음).
    -- "내 장바구니 조회"(WHERE customer_id = ?)가 매 화면 진입마다 발생하는 조회라
    -- 유니크 인덱스로 조회와 중복 방지를 동시에 해결한다.
    CONSTRAINT uk_cart_customer_id UNIQUE (customer_id)
);

CREATE TABLE cart_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id     BIGINT       NOT NULL,
    menu_id     BIGINT       NOT NULL,
    menu_name   VARCHAR(100) NOT NULL,
    menu_price  BIGINT       NOT NULL,
    quantity    INT          NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,

    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES cart (id)
);

-- 장바구니 상세 조회 시 그 장바구니의 항목 목록을 조회(WHERE cart_id = ?)하는
-- 가장 빈번한 경로라 인덱스 필요.
CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);
