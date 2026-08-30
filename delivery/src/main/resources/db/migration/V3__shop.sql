-- shop 모듈: shop, menu_group, menu, menu_option_group, menu_option
-- 모듈 간 FK 금지 원칙에 따라 다른 모듈(auth 등) 테이블과 FK를 걸지 않는다.
-- business_hours, order_ticket, owner_profile은 커밋 27 이후에 추가한다.

CREATE TABLE shop (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id      BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    address       VARCHAR(255) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL
);

-- 사장님(owner_id)이 자기 상점 목록을 조회(WHERE owner_id = ?)하는 화면이 있어 인덱스 필요.
CREATE INDEX idx_shop_owner_id ON shop (owner_id);

CREATE TABLE menu_group (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id       BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    display_order INT          NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    CONSTRAINT fk_menu_group_shop FOREIGN KEY (shop_id) REFERENCES shop (id)
);

-- 상점 상세 화면에서 그 상점의 메뉴 그룹 목록을 조회(WHERE shop_id = ?)하는 경로가 있어 인덱스 필요.
CREATE INDEX idx_menu_group_shop_id ON menu_group (shop_id);

CREATE TABLE menu (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id       BIGINT         NOT NULL,
    menu_group_id BIGINT         NOT NULL,
    name          VARCHAR(100)   NOT NULL,
    description   VARCHAR(500),
    price         BIGINT         NOT NULL,
    sold_out      BOOLEAN        NOT NULL DEFAULT FALSE,
    display_order INT            NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,

    CONSTRAINT fk_menu_shop FOREIGN KEY (shop_id) REFERENCES shop (id),
    CONSTRAINT fk_menu_menu_group FOREIGN KEY (menu_group_id) REFERENCES menu_group (id)
);

-- 상점 상세(고객용) 화면이 shop_id로 그 상점의 메뉴 전체를 조회하는 가장 빈번한 경로.
CREATE INDEX idx_menu_shop_id ON menu (shop_id);
-- 메뉴 그룹별 목록 조회(WHERE menu_group_id = ?) 화면에 필요.
CREATE INDEX idx_menu_menu_group_id ON menu (menu_group_id);

CREATE TABLE menu_option_group (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id       BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    required      BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT          NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,

    CONSTRAINT fk_menu_option_group_menu FOREIGN KEY (menu_id) REFERENCES menu (id)
);

-- 메뉴 상세 조회 시 그 메뉴의 옵션 그룹 목록을 조회(WHERE menu_id = ?)하는 경로가 있어 인덱스 필요.
CREATE INDEX idx_menu_option_group_menu_id ON menu_option_group (menu_id);

CREATE TABLE menu_option (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_option_group_id BIGINT      NOT NULL,
    name                VARCHAR(100) NOT NULL,
    price               BIGINT       NOT NULL,
    display_order       INT          NOT NULL,
    created_at          DATETIME(6)  NOT NULL,
    updated_at          DATETIME(6)  NOT NULL,

    CONSTRAINT fk_menu_option_menu_option_group FOREIGN KEY (menu_option_group_id) REFERENCES menu_option_group (id)
);

-- 옵션 그룹별 옵션 목록 조회(WHERE menu_option_group_id = ?)에 필요.
CREATE INDEX idx_menu_option_menu_option_group_id ON menu_option (menu_option_group_id);
