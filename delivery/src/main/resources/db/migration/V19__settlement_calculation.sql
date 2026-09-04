-- settlement 모듈: 정산 계산 서비스(상점) 지원을 위한 컬럼/인덱스 추가

-- 직전 달 정산액이 음수였을 때 이번 달로 이월된 금액. 특정 주문에 묶이지 않는 값이라
-- SettlementItem(orderId NOT NULL)이 아니라 Settlement 자체에 필드로 둔다.
ALTER TABLE settlement ADD COLUMN carried_over_amount BIGINT NOT NULL DEFAULT 0;

-- 환불 항목의 요율을 정할 때 "같은 주문의 과거 판매(SALE) 항목"을 찾는 조회
-- (WHERE order_id=? AND type=? ORDER BY created_at DESC LIMIT 1)가 필요해 인덱스 추가.
-- 등호 조건(order_id, type)을 앞에, 정렬 기준(created_at)을 뒤에 둔다.
CREATE INDEX idx_settlement_item_order_type_created ON settlement_item (order_id, type, created_at);

-- 정산 계산이 결제 환불 내역을 "해당 상점 & REFUNDED 상태 & 기간" 조건으로 조회한다
-- (ShopSettlementSourceRepository.findRefundedPaymentAmounts). status로 먼저 좁히고
-- updated_at 범위로 거르는 경로라 등호 조건(status)을 앞에 둔다.
CREATE INDEX idx_payment_status_updated_at ON payment (status, updated_at);
