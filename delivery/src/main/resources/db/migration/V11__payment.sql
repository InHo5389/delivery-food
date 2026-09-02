-- order 모듈: payment (결제 도메인, 커밋 36)
-- order_id는 같은 order 모듈 내 orders 테이블을 가리키지만, 아직 orders에 대한
-- 저장 계층(OrderRepository)이 없는 시점이라 FK를 걸지 않는다. Order/Payment를
-- 각자 독립적으로 완결시킨 뒤(주문 생성 API, 커밋 37) 필요하면 그때 FK를 추가한다.

CREATE TABLE payment (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT      NOT NULL,
    amount     BIGINT      NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    -- 주문 1건당 결제는 1건만 존재한다. 결제 상태 조회(WHERE order_id = ?)가
    -- 주문 상세 화면마다 발생하는 빈번한 조회라 유니크 인덱스로 조회와 중복 방지를 겸한다.
    CONSTRAINT uk_payment_order_id UNIQUE (order_id)
);
