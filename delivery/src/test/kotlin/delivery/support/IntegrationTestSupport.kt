package delivery.support

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IntegrationTestSupport {

    companion object {
        // 여러 테스트 클래스가 상속해도 JVM 전체에서 하나만 뜨도록 object로 감싸고 명시적으로 start() 한다.
        // @Testcontainers + @Container(companion object의 static 필드)는 상속 구조에서
        // 서브클래스가 필드를 인식하지 못해 컨테이너가 시작되지 않는 경우가 있어 쓰지 않는다.
        private val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withDatabaseName("delivery")
            .withUsername("test")
            .withPassword("test")
            .apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.flyway.url", mysql::getJdbcUrl)
            registry.add("spring.flyway.user", mysql::getUsername)
            registry.add("spring.flyway.password", mysql::getPassword)
        }
    }
}
