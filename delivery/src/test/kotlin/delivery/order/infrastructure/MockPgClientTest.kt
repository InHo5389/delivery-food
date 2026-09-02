package delivery.order.infrastructure

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MockPgClientTest {

    @Test
    fun `실패율이 0이면 항상 승인된다`() {
        val client = MockPgClient(failureRate = 0.0)

        val results = (1..100).map { client.authorize(1L, 8000L) }

        assertTrue(results.all { it })
    }

    @Test
    fun `실패율이 1이면 항상 거절된다`() {
        val client = MockPgClient(failureRate = 1.0)

        val results = (1..100).map { client.authorize(1L, 8000L) }

        assertTrue(results.none { it })
    }

    @Test
    fun `금액이 0이면 예외가 발생한다`() {
        val client = MockPgClient(failureRate = 0.0)

        assertFailsWith<IllegalArgumentException> { client.authorize(1L, 0L) }
    }

    @Test
    fun `금액이 음수이면 예외가 발생한다`() {
        val client = MockPgClient(failureRate = 0.0)

        assertFailsWith<IllegalArgumentException> { client.authorize(1L, -1000L) }
    }
}
