-- delivery 모듈: rider, delivery, dispatch_offer (배차 도메인 엔티티)
-- order_id/shop_id/account_id는 다른 모듈(order/shop/auth) 테이블에 대한 논리 참조다.
-- 모듈 간 FK 금지 원칙(설계 원칙 5절)에 따라 FK를 걸지 않는다.

-- 라이더 위치·상태. 배차 매칭 엔진이 5초마다 AVAILABLE 라이더 중 반경 내 후보를
-- 찾는 대상 테이블이다.
CREATE TABLE rider (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id            BIGINT        NOT NULL,
    latitude              DECIMAL(10,7) NOT NULL,
    longitude             DECIMAL(10,7) NOT NULL,
    status                VARCHAR(20)   NOT NULL,
    recent_delivery_count INT           NOT NULL DEFAULT 0,
    acceptance_rate       DECIMAL(5,4)  NOT NULL DEFAULT 1.0000,
    available_since       DATETIME(6)   NULL,
    created_at            DATETIME(6)   NOT NULL,
    updated_at            DATETIME(6)   NOT NULL
);

-- 배차 매칭 스케줄러가 매 사이클마다 "지금 배차 가능한 라이더"를 조회(WHERE status = 'AVAILABLE')
-- 하는 것이 가장 빈번한 경로라 인덱스 필요. 반경 계산(ST_Distance_Sphere)은 상점 목록 조회와 동일하게
-- 의도적으로 인덱스를 타지 못하지만(Phase 3에서 Redis GEO로 개선 예정), status 조건만은
-- 이 인덱스로 먼저 걸러낼 수 있다.
CREATE INDEX idx_rider_status ON rider (status);

-- 주문 1건당 배달은 1건. shop의 위치를 pickup 좌표로 스냅샷해 조회 시 shop 모듈을 다시
-- 호출하지 않는다(팬인 0 원칙, Order의 스냅샷 설계와 동일).
CREATE TABLE delivery (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id         BIGINT        NOT NULL,
    shop_id          BIGINT        NOT NULL,
    pickup_latitude  DECIMAL(10,7) NOT NULL,
    pickup_longitude DECIMAL(10,7) NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    rider_id         BIGINT        NULL,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,

    -- rider_id는 같은 delivery 모듈 내 참조라 FK를 허용한다(설계 원칙 5절 — 모듈 간 FK만 금지).
    CONSTRAINT fk_delivery_rider FOREIGN KEY (rider_id) REFERENCES rider (id),
    CONSTRAINT uk_delivery_order_id UNIQUE (order_id)
);

-- 배차 스케줄러가 매 사이클마다 배차 대기 중인 배달을 조회(WHERE status = 'PENDING')하는
-- 핵심 경로라 인덱스 필요.
CREATE INDEX idx_delivery_status ON delivery (status);

-- 배차 1건당 여러 라이더에게 오퍼가 나갈 수 있다(상위 N명). delivery_id, rider_id 모두
-- 같은 delivery 모듈 내 참조라 FK를 허용한다.
CREATE TABLE dispatch_offer (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    delivery_id  BIGINT       NOT NULL,
    rider_id     BIGINT       NOT NULL,
    score        DECIMAL(6,4) NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    offered_at   DATETIME(6)  NOT NULL,
    responded_at DATETIME(6)  NULL,

    CONSTRAINT fk_dispatch_offer_delivery FOREIGN KEY (delivery_id) REFERENCES delivery (id),
    CONSTRAINT fk_dispatch_offer_rider FOREIGN KEY (rider_id) REFERENCES rider (id)
);

-- 다음 배차 사이클에서 "이미 이 배달에 오퍼한 라이더"를 제외하려고 배달 단위로 오퍼 목록을
-- 조회(WHERE delivery_id = ?)하는 것이 매 사이클 반복되는 경로라 인덱스 필요.
CREATE INDEX idx_dispatch_offer_delivery_id ON dispatch_offer (delivery_id);
