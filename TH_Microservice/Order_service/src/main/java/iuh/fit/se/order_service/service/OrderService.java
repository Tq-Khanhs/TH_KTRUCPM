package iuh.fit.se.order_service.service;


import iuh.fit.se.order_service.client.CustomerClient;
import iuh.fit.se.order_service.client.ProductClient;
import iuh.fit.se.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final CustomerClient customerClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient,
                        CustomerClient customerClient,
                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.customerClient = customerClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional
    public Order createOrder(Order order) {
        // Validate customer exists
        var customerResponse = customerClient.getCustomerById(order.getCustomerId());
        if (!customerResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Customer not found");
        }

        // Set order date and initial status
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        // Calculate total and validate products
        double total = 0.0;
        for (OrderItem item : order.getItems()) {
            var productResponse = productClient.getProductById(item.getProductId());
            if (!productResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Product not found: " + item.getProductId());
            }

            ProductDto product = productResponse.getBody();
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + item.getProductId());
            }

            // Update product price from current price
            item.setPrice(product.getPrice());
            total += item.getPrice() * item.getQuantity();

            // Update stock
            productClient.updateStock(item.getProductId(), item.getQuantity());
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        // Send order created event
        kafkaTemplate.send("order-created", savedOrder);

        return savedOrder;
    }

    @Transactional
    public Optional<Order> updateOrderStatus(Long id, String status) {
        return orderRepository.findById(id)
                .map(existingOrder -> {
                    existingOrder.setStatus(status);
                    Order updatedOrder = orderRepository.save(existingOrder);

                    // Send order updated event
                    kafkaTemplate.send("order-updated", updatedOrder);

                    return updatedOrder;
                });
    }

    @Transactional
    public boolean cancelOrder(Long id) {
        return orderRepository.findById(id)
                .map(order -> {
                    if ("PENDING".equals(order.getStatus()) || "PROCESSING".equals(order.getStatus())) {
                        order.setStatus("CANCELLED");
                        orderRepository.save(order);

                        // Return items to inventory
                        for (OrderItem item : order.getItems()) {
                            productClient.updateStock(item.getProductId(), -item.getQuantity());
                        }

                        // Send order cancelled event
                        kafkaTemplate.send("order-cancelled", order);

                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}