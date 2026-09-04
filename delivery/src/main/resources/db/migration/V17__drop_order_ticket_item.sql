-- order_ticket_item은 order_items(order 모듈)를 그대로 복제한 것이었다. 지금은
-- 모놀리스라 티켓 상세 조회 시 order 모듈 Service를 통해 그때그때 항목을 가져와도
-- 비용이 크지 않고, 스냅샷을 유지하면 두 테이블이 영원히 같은 값을 들고 있어야 하는
-- 중복만 생겨서 제거한다(Phase 5에서 원격 호출 비용이 문제가 되면 그때 다시 캐시/
-- 스냅샷 형태로 되돌린다).
DROP TABLE order_ticket_item;
