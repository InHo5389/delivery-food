-- 배차 정책 단순화: 거리/최근 처리 건수/수락률/대기시간 가중치 점수화 대신, 반경 내
-- AVAILABLE 라이더 전원에게 오퍼를 보내고 먼저 수락하는 사람이 배정되는 선착순 방식으로
-- 바꿨다. 더 이상 계산/저장하지 않는 컬럼을 정리한다.
ALTER TABLE dispatch_offer DROP COLUMN score;
ALTER TABLE rider DROP COLUMN recent_delivery_count;
ALTER TABLE rider DROP COLUMN acceptance_rate;
ALTER TABLE rider DROP COLUMN available_since;
