-- shop 모듈: 메뉴 이미지 URL 컬럼 추가
-- 별도 인덱스는 두지 않는다 — image_url로 검색/필터링하는 조회 경로가 없다.

ALTER TABLE menu
    ADD COLUMN image_url VARCHAR(255) NULL AFTER description;
