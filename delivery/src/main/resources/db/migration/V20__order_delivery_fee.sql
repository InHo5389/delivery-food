-- order 모듈: orders.delivery_fee 추가
-- shop.delivery_fee는 상점의 "현재" 설정값일 뿐, 지금까지는 주문 시점에 스냅샷되지 않았다.
-- 상점이 나중에 배달비를 바꾸면 과거 주문이 실제로 얼마를 청구했는지 더 이상 알 수 없게
-- 되고, 라이더 정산(53-3)이 "이 배달로 얼마를 지급해야 하는지" 판단할 근거도 사라진다.
-- customer_name/customer_phone과 동일한 스냅샷 원리로 주문 생성 시점 값을 그대로 저장한다.
ALTER TABLE orders ADD COLUMN delivery_fee BIGINT NOT NULL DEFAULT 0;
