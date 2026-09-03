package delivery

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class DeliveryApplication

fun main(args: Array<String>) {
	runApplication<DeliveryApplication>(*args)
}
