-- order 모듈: orders (주문 도메인 엔티티, 커밋 33)
-- shop_id, menu_id는 다른 모듈(shop) 테이블에 대한 논리 참조다.
-- 모듈 간 FK 금지 원칙(1-8절)에 따라 FK를 걸지 않는다.
-- menu_name/menu_price, customer_name/customer_phone은 주문 시점 스냅샷이라
-- shop/auth 테이블을 다시 조회할 필요가 없다(팬인 0의 근거).

CREATE TABLE orders (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT       NOT NULL,
    shop_id        BIGINT       NOT NULL,
    menu_id        BIGINT       NOT NULL,
    menu_name      VARCHAR(100) NOT NULL,
    menu_price     BIGINT       NOT NULL,
    quantity       INT          NOT NULL,
    customer_name  VARCHAR(50)  NOT NULL,
    customer_phone VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL
);

-- 고객이 "내 주문 내역"을 조회(WHERE customer_id = ?)하는 화면이 있어 인덱스 필요.
CREATE INDEX idx_orders_customer_id ON orders (customer_id);

-- 사장님이 "우리 상점 주문 목록"을 조회(WHERE shop_id = ?)하는 화면이 있어 인덱스 필요.
CREATE INDEX idx_orders_shop_id ON orders (shop_id);
