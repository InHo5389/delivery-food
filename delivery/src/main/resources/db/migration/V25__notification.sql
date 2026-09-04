-- notification 모듈: 주문 상태 변경을 사용자에게 SSE로 실시간 푸시하고(커밋 54~63),
-- 연결이 끊겨 있던 동안 놓친 알림은 이 테이블로 이력 조회할 수 있게 한다.
-- order_id는 order 모듈 테이블에 대한 논리 참조라 FK를 걸지 않는다(모듈 간 FK 금지).
CREATE TABLE notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    order_id    BIGINT       NOT NULL,
    message     VARCHAR(200) NOT NULL,
    created_at  DATETIME(6)  NOT NULL
);

-- "내 알림 목록"을 최신순으로 조회하는 화면이 있어 인덱스 필요.
-- 등호 조건(user_id)을 앞에, 정렬 조건(created_at)을 뒤에 둔다.
CREATE INDEX idx_notification_user_created ON notification (user_id, created_at);
