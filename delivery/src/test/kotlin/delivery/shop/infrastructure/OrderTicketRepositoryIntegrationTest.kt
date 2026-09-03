package delivery.shop.infrastructure

import delivery.shop.domain.OrderTicket
import delivery.shop.domain.OrderTicketItem
import delivery.shop.domain.Shop
import delivery.support.IntegrationTestSupport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderTicketRepositoryIntegrationTest(
    @Autowired private val shopRepository: ShopRepository,
    @Autowired private val orderTicketRepository: OrderTicketRepository,
    @Autowired private val orderTicketItemRepository: OrderTicketItemRepository,
) : IntegrationTestSupport() {

    private fun newShop(): Shop = shopRepository.save(
        Shop(System.nanoTime(), "가게", "서울", BigDecimal("37.5665000"), BigDecimal("126.9780000"), "0212345678")
    )

    @Test
    fun `orderId로 티켓을 조회할 수 있다`() {
        val shop = newShop()
        val ticket = orderTicketRepository.save(OrderTicket(orderId = 100L, shopId = shop.id!!, customerName = "홍길동", totalAmount = 8000L))

        val actual = orderTicketRepository.findByOrderId(100L)

        assertEquals(ticket.id, actual?.id)
    }

    @Test
    fun `존재하지 않는 orderId로 조회하면 null을 반환한다`() {
        val actual = orderTicketRepository.findByOrderId(999_999L)

        assertNull(actual)
    }

    @Test
    fun `상점별 티켓 목록을 최신순으로 조회한다`() {
        val shop = newShop()
        orderTicketRepository.save(OrderTicket(orderId = 1L, shopId = shop.id!!, customerName = "A", totalAmount = 1000L))
        val second = orderTicketRepository.save(OrderTicket(orderId = 2L, shopId = shop.id!!, customerName = "B", totalAmount = 2000L))

        val actual = orderTicketRepository.findAllByShopIdOrderByCreatedAtDesc(shop.id!!)

        assertEquals(2, actual.size)
        assertEquals(second.id, actual[0].id)
    }

    @Test
    fun `티켓 항목을 티켓 단위로 조회한다`() {
        val shop = newShop()
        val ticket = orderTicketRepository.save(OrderTicket(orderId = 1L, shopId = shop.id!!, customerName = "A", totalAmount = 16000L))
        orderTicketItemRepository.save(OrderTicketItem(orderTicketId = ticket.id!!, menuName = "짜장면", menuPrice = 8000L, quantity = 2))

        val actual = orderTicketItemRepository.findAllByOrderTicketId(ticket.id!!)

        assertTrue(actual.any { it.menuName == "짜장면" })
    }
}
