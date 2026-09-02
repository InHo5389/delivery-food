-- order 모듈: payment (결제 도메인, 커밋 36)
-- order_id는 같은 order 모듈 내 참조라 FK를 허용한다(1-8절 — 모듈 간 FK만 금지).
-- 주문(orders)은 항목이 몇 개든 결제 1건과 1:1로 묶인다(Order/OrderItem 분리, 커밋 39).

CREATE TABLE payment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT      NOT NULL,
    amount     BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id),
    -- 주문 1건당 결제는 1건만 존재한다. 결제 상태 조회(WHERE order_id = ?)가
    -- 주문 상세 화면마다 발생하는 빈번한 조회라 유니크 인덱스로 조회와 중복 방지를 겸한다.
    CONSTRAINT uk_payment_order_id UNIQUE (order_id)
);
