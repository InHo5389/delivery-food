-- shop 모듈: 상점 목록 조회(거리 기반 정렬)를 위한 위경도 컬럼 추가

ALTER TABLE shop
    ADD COLUMN latitude  DECIMAL(10, 7) NOT NULL AFTER address,
    ADD COLUMN longitude DECIMAL(10, 7) NOT NULL AFTER latitude;

-- 상점 목록 조회 시 영업중(status=OPEN)인 상점만 걸러서 거리 계산 대상으로 좁힌다.
-- 위경도 자체(ST_Distance_Sphere)는 함수 계산이라 B-tree 인덱스를 타지 않지만,
-- status 필터만으로도 대상 행 수를 크게 줄일 수 있어 걸어둔다.
-- 실제 거리 기반 검색 최적화(Geohash, Redis GEO)는 Phase 3에서 다룬다.
CREATE INDEX idx_shop_status ON shop (status);
