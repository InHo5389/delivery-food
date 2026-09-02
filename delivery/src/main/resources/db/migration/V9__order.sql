-- order 모듈: orders, order_items (주문 도메인 엔티티, 커밋 33)
-- shop_id는 다른 모듈(shop) 테이블에 대한 논리 참조다. 모듈 간 FK 금지 원칙(1-8절)에 따라
-- FK를 걸지 않는다. customer_name/customer_phone은 주문 시점 스냅샷이라 auth 테이블을
-- 다시 조회할 필요가 없다(팬인 0의 근거).
--
-- 장바구니에 담긴 메뉴 여러 개는 주문 1건(orders) + 여러 항목(order_items)으로 모델링한다.
-- 결제(payment)도 주문 1건 단위로 묶이므로, 취소/환불을 주문 전체 단위로 정확히
-- 처리할 수 있다(커밋 39).

CREATE TABLE orders (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id    BIGINT       NOT NULL,
    shop_id        BIGINT       NOT NULL,
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

-- order_id는 같은 order 모듈 내 참조라 FK를 허용한다(1-8절 — 모듈 간 FK만 금지).
-- menu_name/menu_price는 주문 시점 스냅샷이다("주문 당시엔 8,000원" 문제 방지).
CREATE TABLE order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT       NOT NULL,
    menu_id    BIGINT       NOT NULL,
    menu_name  VARCHAR(100) NOT NULL,
    menu_price BIGINT       NOT NULL,
    quantity   INT          NOT NULL,
    created_at DATETIME(6)  NOT NULL,

    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

-- 주문 상세 조회 시 그 주문의 항목 목록을 조회(WHERE order_id = ?)하는
-- 가장 빈번한 경로라 인덱스 필요.
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
