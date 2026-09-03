-- delivery 모듈: 예상 픽업 시각. 사장님이 주문 접수 시 입력하는 조리 예상 시간을 기준으로
-- 계산해 저장한다. 라이더가 배차 큐를 볼 때 "언제쯤 가면 되는지" 판단하는 근거가 된다.
-- 현재는 조회(정렬/필터) 조건으로 쓰이지 않으므로 인덱스는 만들지 않는다.
ALTER TABLE delivery ADD COLUMN estimated_pickup_at DATETIME(6) NULL;
