-- shop 모듈: order_ticket, order_ticket_item (사장님의 주문 접수·조리 관리 큐)
-- order_id는 다른 모듈(order) 테이블에 대한 논리 참조라 FK를 걸지 않는다.
-- customer_name/total_amount는 주문 생성 시점 스냅샷이라 조회 시 order 모듈을
-- 다시 호출하지 않는다(팬인 0 원칙, Order/OrderItem 스냅샷 설계와 동일).
--
-- shop_id는 같은 shop 모듈 내 참조라 FK를 허용한다(모듈 간 FK만 금지).

CREATE TABLE order_ticket (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT       NOT NULL,
    shop_id       BIGINT       NOT NULL,
    customer_name VARCHAR(50)  NOT NULL,
    total_amount  BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    CONSTRAINT fk_order_ticket_shop FOREIGN KEY (shop_id) REFERENCES shop (id),
    CONSTRAINT uk_order_ticket_order_id UNIQUE (order_id)
);

-- 사장님이 "우리 상점의 접수 대기 목록"을 조회(WHERE shop_id = ?)하는 화면이 있어 인덱스 필요.
CREATE INDEX idx_order_ticket_shop_id ON order_ticket (shop_id);

-- order_ticket_id는 같은 shop 모듈 내 참조라 FK를 허용한다.
-- menu_name/menu_price는 주문 시점 스냅샷이다(Order/OrderItem과 동일한 이유).
CREATE TABLE order_ticket_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_ticket_id BIGINT       NOT NULL,
    menu_name       VARCHAR(100) NOT NULL,
    menu_price      BIGINT       NOT NULL,
    quantity        INT          NOT NULL,
    created_at      DATETIME(6)  NOT NULL,

    CONSTRAINT fk_order_ticket_item_ticket FOREIGN KEY (order_ticket_id) REFERENCES order_ticket (id)
);

-- 티켓 상세 조회 시 그 티켓의 항목 목록을 조회(WHERE order_ticket_id = ?)하는 경로라 인덱스 필요.
CREATE INDEX idx_order_ticket_item_ticket_id ON order_ticket_item (order_ticket_id);
