package iuh.fit.se.shipping_service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import iuh.fit.se.shipping_service.client.CustomerClient;
import iuh.fit.se.shipping_service.client.CustomerDto;
import iuh.fit.se.shipping_service.client.OrderClient;
import iuh.fit.se.shipping_service.client.OrderDto;
import iuh.fit.se.shipping_service.model.Shipment;
import iuh.fit.se.shipping_service.repository.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class ShippingService {

    private static final Logger logger = LoggerFactory.getLogger(ShippingService.class);
    private final ShipmentRepository shipmentRepository;
    private final OrderClient orderClient;
    private final CustomerClient customerClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public ShippingService(ShipmentRepository shipmentRepository,
                           OrderClient orderClient,
                           CustomerClient customerClient,
                           KafkaTemplate<String, Object> kafkaTemplate) {
        this.shipmentRepository = shipmentRepository;
        this.orderClient = orderClient;
        this.customerClient = customerClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Shipment> getAllShipments() {
        return shipmentRepository.findAll();
    }

    public Optional<Shipment> getShipmentById(Long id) {
        return shipmentRepository.findById(id);
    }

    public List<Shipment> getShipmentsByOrderId(Long orderId) {
        return shipmentRepository.findByOrderId(orderId);
    }

    public Optional<Shipment> getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber);
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "createShipmentFallback")
    @Retry(name = "orderService", fallbackMethod = "createShipmentFallback")
    @RateLimiter(name = "orderService")
    @TimeLimiter(name = "orderService")
    @Transactional
    public CompletableFuture<Shipment> createShipment(Shipment shipment) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate order exists
            var orderResponse = orderClient.getOrderById(shipment.getOrderId());
            if (!orderResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Order not found: " + shipment.getOrderId());
            }

            OrderDto order = orderResponse.getBody();

            // Get customer address
            var customerResponse = customerClient.getCustomerById(order.getCustomerId());
            if (!customerResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Customer not found: " + order.getCustomerId());
            }

            CustomerDto customer = customerResponse.getBody();

            // Set shipment details
            shipment.setTrackingNumber(generateTrackingNumber());
            shipment.setStatus("PENDING");
            shipment.setShippingAddress(customer.getAddress());
            shipment.setCreatedAt(LocalDateTime.now());
            shipment.setUpdatedAt(LocalDateTime.now());
            shipment.setEstimatedDelivery(LocalDateTime.now().plusDays(3));

            Shipment savedShipment = shipmentRepository.save(shipment);

            // Update order status
            orderClient.updateOrderStatus(shipment.getOrderId(), "SHIPPING");

            // Send shipment created event
            kafkaTemplate.send("shipment-created", savedShipment);

            return savedShipment;
        });
    }

    public CompletableFuture<Shipment> createShipmentFallback(Shipment shipment, Exception ex) {
        logger.error("Fallback executed for createShipment. Error: {}", ex.getMessage());

        // Create a temporary shipment record
        shipment.setTrackingNumber(generateTrackingNumber() + "-TEMP");
        shipment.setStatus("PENDING_CONFIRMATION");
        shipment.setCreatedAt(LocalDateTime.now());
        shipment.setUpdatedAt(LocalDateTime.now());

        Shipment savedShipment = shipmentRepository.save(shipment);

        // Send to a special topic for manual processing
        kafkaTemplate.send("shipment-fallback", savedShipment);

        return CompletableFuture.completedFuture(savedShipment);
    }

    private String generateTrackingNumber() {
        return "TRK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "updateShipmentStatusFallback")
    @Retry(name = "orderService", fallbackMethod = "updateShipmentStatusFallback")
    @Transactional
    public Shipment updateShipmentStatus(Long id, String status) {
        return shipmentRepository.findById(id)
                .map(shipment -> {
                    shipment.setStatus(status);
                    shipment.setUpdatedAt(LocalDateTime.now());

                    if ("DELIVERED".equals(status)) {
                        shipment.setActualDelivery(LocalDateTime.now());
                        // Update order status
                        orderClient.updateOrderStatus(shipment.getOrderId(), "DELIVERED");
                    } else if ("SHIPPED".equals(status)) {
                        // Update order status
                        orderClient.updateOrderStatus(shipment.getOrderId(), "SHIPPED");
                    }

                    Shipment updatedShipment = shipmentRepository.save(shipment);

                    // Send shipment updated event
                    kafkaTemplate.send("shipment-updated", updatedShipment);

                    return updatedShipment;
                })
                .orElseThrow(() -> new RuntimeException("Shipment not found: " + id));
    }

    public Shipment updateShipmentStatusFallback(Long id, String status, Exception ex) {
        logger.error("Fallback executed for updateShipmentStatus. ShipmentId: {}, Status: {}, Error: {}",
                id, status, ex.getMessage());

        return shipmentRepository.findById(id)
                .map(shipment -> {
                    shipment.setStatus(status + "_PENDING");
                    shipment.setUpdatedAt(LocalDateTime.now());
                    Shipment updatedShipment = shipmentRepository.save(shipment);

                    // Send to a special topic for manual processing
                    kafkaTemplate.send("shipment-status-update-fallback", Map.of(
                            "shipmentId", id,
                            "status", status,
                            "orderId", shipment.getOrderId()
                    ));

                    return updatedShipment;
                })
                .orElseThrow(() -> new RuntimeException("Shipment not found: " + id));
    }

    @KafkaListener(topics = "payment-completed", groupId = "shipping-group")
    public void handlePaymentCompleted(Map<String, Object> paymentData) {
        try {
            Long orderId = Long.valueOf(paymentData.get("orderId").toString());

            // Create shipment for the paid order
            Shipment shipment = new Shipment();
            shipment.setOrderId(orderId);
            shipment.setCarrier("DEFAULT_CARRIER");

            createShipment(shipment);
        } catch (Exception e) {
            logger.error("Error processing payment-completed event", e);
        }
    }
}
