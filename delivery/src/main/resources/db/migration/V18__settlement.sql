-- settlement 모듈: settlement(정산 합계) + settlement_item(주문별 내역) + commission_rate(요율 이력)
-- target_id는 target_type에 따라 shop 모듈의 shop.id 또는 delivery 모듈의 rider.id를 가리키는
-- 논리 참조다(다형적 참조라 단일 FK로 표현할 수 없기도 하고, 모듈 경계 규칙상 FK 금지이기도 하다).

CREATE TABLE settlement (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type   VARCHAR(10)  NOT NULL,
    target_id     BIGINT       NOT NULL,
    period_start  DATETIME(6)  NOT NULL,
    period_end    DATETIME(6)  NOT NULL,
    total_amount  BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    -- 같은 대상·같은 기간의 정산이 중복 생성되는 것을 막는다(배치가 중복 실행돼도 1건만 남게).
    CONSTRAINT uk_settlement_target_period UNIQUE (target_type, target_id, period_start, period_end)
);

-- 사장님/라이더가 "내 정산 내역"을 기간순으로 조회하는 것이 주 조회 경로라 인덱스 필요.
-- 등호 조건(target_type, target_id)을 앞에, 범위/정렬 조건(period_start)을 뒤에 둔다.
CREATE INDEX idx_settlement_target ON settlement (target_type, target_id, period_start);

-- settlement_id는 같은 settlement 모듈 내 참조라 FK를 허용한다(모듈 간 FK만 금지).
-- order_id는 다른 모듈(order) 테이블에 대한 논리 참조라 FK를 걸지 않는다.
CREATE TABLE settlement_item (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_id      BIGINT        NOT NULL,
    order_id           BIGINT        NOT NULL,
    type               VARCHAR(10)   NOT NULL,
    amount             BIGINT        NOT NULL,
    applied_fee_rate   DECIMAL(5,4)  NOT NULL,
    settlement_amount  BIGINT        NOT NULL,
    created_at         DATETIME(6)   NOT NULL,

    CONSTRAINT fk_settlement_item_settlement FOREIGN KEY (settlement_id) REFERENCES settlement (id)
);

-- 정산 상세 화면에서 그 정산에 속한 항목 목록을 조회(WHERE settlement_id = ?)하는 경로라 인덱스 필요.
CREATE INDEX idx_settlement_item_settlement_id ON settlement_item (settlement_id);

-- rate_type은 요율의 종류(현재는 PLATFORM_FEE 하나뿐이나 향후 종류가 늘어날 수 있어 이력을
-- 종류별로 분리해둔다). effective_to를 따로 두지 않는다 — 새 요율 추가 시점이 곧 이전 요율의
-- 종료 시점이라, 별도 컬럼을 두면 두 값이 어긋날 여지만 생긴다(CommissionRate.kt 주석 참조).
CREATE TABLE commission_rate (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    rate_type       VARCHAR(30)   NOT NULL,
    rate            DECIMAL(5,4)  NOT NULL,
    effective_from  DATETIME(6)   NOT NULL,
    created_at      DATETIME(6)   NOT NULL
);

-- "이 시점에 유효했던 가장 최근 요율"을 찾는 조회(WHERE rate_type=? AND effective_from<=?
-- ORDER BY effective_from DESC LIMIT 1)가 유일한 조회 경로라 인덱스 필요.
-- 등호 조건(rate_type)을 앞에, 범위/정렬 조건(effective_from)을 뒤에 둔다.
CREATE INDEX idx_commission_rate_type_effective ON commission_rate (rate_type, effective_from);
