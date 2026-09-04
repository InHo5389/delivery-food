package delivery.auth.domain

enum class Role {
    CUSTOMER,
    OWNER,
    RIDER,

    // 회원가입으로는 발급되지 않는다(AuthService.signup 참조) — 운영자 계정은 DB에
    // 직접 role을 심어서만 만든다. 누구나 자칭 운영자로 가입할 수 있게 하는 구멍을
    // 막기 위함(정산 확정/지급 같은 민감한 admin API의 권한 기준이 된다).
    ADMIN,
}
