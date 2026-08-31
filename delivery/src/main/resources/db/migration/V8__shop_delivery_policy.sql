-- shop 모듈: 최소주문금액, 배달비 정책 컬럼 추가
-- 조회 조건(WHERE/ORDER BY)에 쓰이지 않는 단순 값 컬럼이라 별도 인덱스는 걸지 않는다.

ALTER TABLE shop
    ADD COLUMN min_order_amount BIGINT NOT NULL DEFAULT 0 AFTER phone,
    ADD COLUMN delivery_fee     BIGINT NOT NULL DEFAULT 0 AFTER min_order_amount;
