-- delivery 모듈: 라이더 정산이 "이 라이더가 이 기간에 완료한 배달"을 조회
-- (WHERE rider_id=? AND status='DELIVERED' AND updated_at>=? AND updated_at<?)하는
-- 새 경로가 생겨 인덱스가 필요하다. 등호 조건(rider_id, status)을 앞에, 범위 조건
-- (updated_at)을 뒤에 둔다.
CREATE INDEX idx_delivery_rider_status_updated_at ON delivery (rider_id, status, updated_at);
