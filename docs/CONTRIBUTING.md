# 커밋 컨벤션

```
<type>(<scope>): <subject>

<body>

Refs: docs/...
```

## type

| type | 용도 |
|---|---|
| `feat` | 새 기능 |
| **`perf`** | **성능 개선 ★ 핵심** |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `test` | 테스트 |
| `docs` | 문서 (측정 리포트 포함) |
| `chore` | 빌드·설정·의존성 |
| `ci` / `build` | CI/CD |

## scope

`auth` `customer` `order` `shop` `delivery` `search` `notification` `infra` `k6` `deploy`

> `auth` = 인증/계정 서비스(구 customer-service) · `customer` = 고객용 프론트엔드/도메인 로직

## `perf:` 커밋 본문 템플릿 (이 프로젝트의 핵심)

```
perf(shop): 상점 상세 Redis 캐싱 적용

Cache-Aside 패턴. Universal Data이므로 로컬 캐시 계층도 함께 검토.

[측정 환경]
앱 2vCPU/2GB×1, MySQL 4vCPU/8GB, Redis 1vCPU/1GB
데이터: shop 10만 / menu 500만 / orders 1,000만
K6 constant-arrival-rate 500rps, 워밍업 2분 후 3회 중앙값

[8대 표준 지표]
지표              Before      After      변화
평균 응답시간      412ms       31ms      -92.5%
p95               620ms       42ms      -93.2%
p99             4,200ms       95ms      -97.7%
TPS                 480      1,850     +285.4%
CPU (앱)            78%        41%      -37%p
Memory (힙)       3.2GB      2.1GB      -34.4%
DB Connection    18/20 활성   3/20 활성  -83.3%
Redis Connection      0         12          -

[트레이드오프]
최대 30초 stale. 영업상태/품절은 별도 관리(다음 커밋).

Refs: docs/phase3/A-05-shop-cache/
```

> **이 습관 하나가 Phase 3의 문서 작성 시간을 절반으로 줄입니다.**
> 나중에 `git log --grep="^perf"` 하나로 성능 리포트가 완성됩니다.

## 커밋 원칙

```
✅ 하나의 커밋 = 하나의 논리적 변경
✅ 각 커밋에서 빌드·테스트 통과 (bisect 가능)
✅ perf 커밋은 반드시 측정 전/후 기록
✅ 실패한 시도도 커밋하고 revert
   → "Redisson 분산락 도입" 후 "Revert: 톰캣 스레드 점유 부작용으로 폐기"
   → 이 히스토리가 실제 고민의 증거

❌ "WIP", "수정", "update", "asdf"
❌ 여러 기능을 한 커밋에
❌ .env, application-prod.yml, *.pem 커밋
```
