package iuh.fit.se.product_service.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @RabbitListener(queues = RabbitMQConfig.PRODUCT_QUEUE)
    public void handleOrderEvent(String message) {
        log.info("Received order event: {}", message);
        // Process the order event
        // Note: Stock updates are handled via REST API calls
    }
}
