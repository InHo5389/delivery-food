-- 미접수 주문 자동 취소 스케줄러가 "PAID 상태로 일정 시간 이상 머문 주문"을
-- 주기적으로 조회(WHERE status = 'PAID' AND updated_at < ?)하는 경로라 인덱스 필요.
-- status가 등호 조건, updated_at이 범위 조건이라 이 순서로 복합 인덱스를 건다.
CREATE INDEX idx_orders_status_updated_at ON orders (status, updated_at);
