# 프로젝트 지침

## 엔티티 생성 시 인덱스 규칙

새 엔티티(테이블)를 만들 때는 반드시 다음을 함께 처리한다.

1. **조회 패턴을 먼저 파악한다.** 어떤 컬럼으로 WHERE, JOIN, ORDER BY 하는지 확인 없이 인덱스를 걸지 않는다.
2. **필요한 인덱스를 Flyway 마이그레이션(`V*__*.sql`)에 함께 작성한다.** 엔티티만 만들고 인덱스는 나중으로 미루지 않는다.
3. **왜 이 인덱스가 필요한지 마이그레이션 파일에 주석으로 남긴다.** 예:
   ```sql
   -- shop_id로 메뉴 목록을 조회하는 화면(상점 상세)이 있어 인덱스 필요
   CREATE INDEX idx_menu_shop_id ON menu (shop_id);
   ```
4. **불필요한 인덱스는 만들지 않는다.** 인덱스는 조회를 빠르게 하지만 쓰기 비용과 저장 공간을 늘린다. "나중에 쓸 것 같아서"로 인덱스를 추가하지 않는다.
5. **복합 인덱스의 컬럼 순서는 카디널리티와 조회 조건(등호 조건이 앞, 범위 조건이 뒤)을 근거로 정하고, 그 근거를 함께 적는다.**
6. **PK는 가이드 문서(`배달앱_MSA_완전실행계획서.md`) 1-9절 원칙에 따라 `AUTO_INCREMENT`를 기본으로 한다.** UUID 등 대안을 쓸 경우 그 이유를 함께 남긴다.

> 성능 개선(Phase 3)에서 실측으로 인덱스 효과를 검증하지만, Phase 0~1(기본틀) 단계에서도 명백히 필요한 인덱스(FK 대체 컬럼, 목록 조회 조건 컬럼 등)는 처음부터 넣는다. "나중에 붙이면 된다"고 미루지 않는다.

## 기능 구현 시 테스트 작성 규칙

코드(API, 서비스 로직 등)를 새로 작성하거나 수정할 때는 구현과 함께 다음 테스트를 반드시 작성한다. 테스트 작성 여부를 사용자에게 묻지 말고 항상 함께 작성한다.

1. **단위 테스트** — 서비스/도메인 로직 단위로 작성한다. 외부 의존성(Repository, 외부 API 등)은 목(mock)으로 대체한다.
2. **경곗값 테스트** — 다음 케이스를 빠짐없이 포함한다.
   - 정상 범위의 최소값/최대값
   - 빈 값, null, 공백 문자열
   - 길이 제한(문자열 필드)의 경계 (제한값, 제한값+1)
   - 중복/이미 존재하는 값 (예: 이메일 중복 가입)
   - 만료/미만료 경계 (예: 토큰 만료 시각 직전/직후)
   - 존재하지 않는 리소스 조회
3. **통합 테스트** — Testcontainers(MySQL 등 실제 인프라 컨테이너)를 사용해 실제 DB/외부 연동까지 포함한 흐름을 검증한다. `@SpringBootTest` + Testcontainers 조합을 기본으로 한다. 목으로 대체하지 않는다.
4. 테스트는 실패 케이스(예외 발생, 잘못된 입력)와 성공 케이스를 모두 포함한다.
5. 테스트 코드도 실제 프로덕션 코드와 동일하게 인덱스/성능을 의식한다 — 예를 들어 대량 데이터 시나리오가 필요한 테스트는 별도로 표시한다(`@Tag("slow")` 등).

> 사용자가 직접 API를 수동으로 호출해 확인하는 것과 별개로, 자동화된 테스트(단위 + 경곗값 + 통합)는 매 기능 구현 시 기본으로 포함되어야 한다.

## 모듈 패키지 구조

각 도메인 모듈(`auth`, `order`, `shop`, `delivery`, `search`, `notification`)은 다음 하위 패키지를 쓴다.

```
{module}/
├── api/
│   ├── dto/         Request, Response (Controller가 주고받는 DTO)
│   └── XxxController.kt
├── domain/          엔티티, 도메인 서비스, ErrorCode
├── application/
│   ├── dto/         Command, Query, 결과 DTO (Service가 주고받는 DTO)
│   └── XxxService.kt
└── infrastructure/  Repository, 외부 연동 클라이언트
```

- **Facade는 쓰지 않는다.** `AuthFacade`처럼 모듈 간 호출을 위한 별도 파사드 계층을 두지 않는다. HTTP 엔드포인트(Controller)는 `presentation`이 아니라 **`api`** 패키지에 둔다.
- 다른 모듈이 이 모듈의 기능이 필요하면 `application`의 Service를 직접 주입해서 쓴다.
- **Command/Request/Response 등 DTO는 Service·Controller 클래스 파일 안에 같이 두지 않는다.** 각각 `application/dto`, `api/dto` 하위에 파일당 클래스 하나로 분리한다.
  - `api/dto` — `SignupRequest`, `LoginRequest`, `TokenResponse` 등 HTTP 계층 전용 DTO. `@field:NotBlank` 같은 요청 검증 애너테이션은 여기에만 붙인다.
  - `application/dto` — `SignupCommand`, `TokenPair` 등 Service 계층 전용 DTO. `api/dto`의 Request가 이 Command로 변환(`toCommand()`)되어 Service에 전달된다.
