-- order_ticket은 shop 모듈이 사장님의 주문 접수·조리 화면을 위해 order 모듈의
-- Order를 사본으로 들고 있던 테이블이었다. 값이 어긋날 일이 없는데도(주문 상태
-- 전이마다 order → shop 콜백으로 계속 동기화해야 했다) 사본을 유지하던 것이라
-- 제거하고, 조회 시 order 모듈이 자기 Order를 상점 관점으로 걸러 직접 돌려주는
-- 방식으로 바꿨다(order_ticket_item을 제거했던 V17과 같은 이유).
DROP TABLE order_ticket;
