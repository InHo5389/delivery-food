-- 만료 배치(커밋 53-11)가 "ISSUED이면서 expiresAt이 지난" 건을 순회하는 조회
-- (WHERE status=? AND expires_at<? ORDER BY expires_at)에 맞춰 idx_issuance_status를
-- 복합 인덱스로 넓힌다. 등호 조건(status)을 앞에, 범위 조건(expires_at)을 뒤에 둔다.
-- 이 인덱스가 상태 단일 조회(findAllByStatus)도 왼쪽 접두사로 그대로 커버해
-- idx_issuance_status는 더 이상 따로 둘 필요가 없다.
DROP INDEX idx_issuance_status ON issuance;
CREATE INDEX idx_issuance_status_expires_at ON issuance (status, expires_at);
