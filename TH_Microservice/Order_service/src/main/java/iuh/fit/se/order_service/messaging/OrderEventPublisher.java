package iuh.fit.se.order_service.messaging;

import com.example.orderservice.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreated(Order order) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
            order
        );
    }

    public void publishOrderCancelled(Order order) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_CANCELLED_ROUTING_KEY,
            order
        );
    }

    public void publishOrderStatusUpdated(Order order) {
        Map<String, Object> message = new HashMap<>();
        message.put("order_id", order.getId());
        message.put("status", order.getStatus());
        message.put("updated_at", order.getUpdatedAt());

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EXCHANGE,
            RabbitMQConfig.ORDER_STATUS_UPDATED_ROUTING_KEY,
            message
        );
    }
}
