package delivery.shop.application

import delivery.common.exception.BusinessException
import delivery.shop.application.dto.CreateOwnerProfileCommand
import delivery.shop.application.dto.UpdateOwnerProfileCommand
import delivery.shop.domain.OwnerProfile
import delivery.shop.domain.ShopErrorCode
import delivery.shop.infrastructure.OwnerProfileRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class OwnerProfileServiceTest {

    private val ownerProfileRepository = mockk<OwnerProfileRepository>()
    private lateinit var ownerProfileService: OwnerProfileService

    @BeforeEach
    fun setUp() {
        ownerProfileService = OwnerProfileService(ownerProfileRepository)
    }

    @Test
    fun `프로필을 생성하면 저장한다`() {
        val command = CreateOwnerProfileCommand(1L, "123-45-67890", "가게상호", "국민은행", "1234-56-789")
        every { ownerProfileRepository.existsByAccountId(1L) } returns false
        val savedSlot = slot<OwnerProfile>()
        every { ownerProfileRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val actual = ownerProfileService.create(command)

        assertEquals("가게상호", actual.businessName)
    }

    @Test
    fun `이미 프로필이 있으면 생성 시 예외가 발생한다`() {
        val command = CreateOwnerProfileCommand(1L, "123-45-67890", "가게상호", "국민은행", "1234-56-789")
        every { ownerProfileRepository.existsByAccountId(1L) } returns true

        val exception = assertThrows<BusinessException> { ownerProfileService.create(command) }

        assertEquals(ShopErrorCode.OWNER_PROFILE_ALREADY_EXISTS, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 프로필을 조회하면 예외가 발생한다`() {
        every { ownerProfileRepository.findByAccountId(999L) } returns null

        val exception = assertThrows<BusinessException> { ownerProfileService.getByAccountId(999L) }

        assertEquals(ShopErrorCode.OWNER_PROFILE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `프로필을 수정하면 필드가 반영된다`() {
        val profile = OwnerProfile.withId(1L, 1L, "123-45-67890", "가게상호", "국민은행", "1234-56-789")
        every { ownerProfileRepository.findByAccountId(1L) } returns profile
        val command = UpdateOwnerProfileCommand("999-99-99999", "새상호", "신한은행", "9999-99-999")

        val actual = ownerProfileService.update(1L, command)

        assertEquals("새상호", actual.businessName)
        assertEquals("신한은행", actual.settlementBank)
    }
}
