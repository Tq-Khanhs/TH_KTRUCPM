package iuh.fit.se.payment_service.service;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import iuh.fit.se.payment_service.client.OrderClient;
import iuh.fit.se.payment_service.client.OrderDto;
import iuh.fit.se.payment_service.model.Payment;
import iuh.fit.se.payment_service.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final OrderClient orderClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          OrderClient orderClient,
                          KafkaTemplate<String, Object> kafkaTemplate) {
        this.paymentRepository = paymentRepository;
        this.orderClient = orderClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "processPaymentFallback")
    @Retry(name = "orderService", fallbackMethod = "processPaymentFallback")
    @RateLimiter(name = "orderService")
    @TimeLimiter(name = "orderService")
    @Transactional
    public CompletableFuture<Payment> processPayment(Payment payment) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate order exists and get amount
            var orderResponse = orderClient.getOrderById(payment.getOrderId());
            if (!orderResponse.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Order not found: " + payment.getOrderId());
            }

            OrderDto order = orderResponse.getBody();

            // Set payment details
            payment.setAmount(order.getTotalAmount());
            payment.setPaymentDate(LocalDateTime.now());
            payment.setStatus("PENDING");
            payment.setTransactionId(UUID.randomUUID().toString());

            // Simulate payment processing with external gateway
            boolean paymentSuccessful = processWithPaymentGateway(payment);

            if (paymentSuccessful) {
                payment.setStatus("COMPLETED");

                // Update order status
                orderClient.updateOrderStatus(payment.getOrderId(), "PAID");

                // Send payment completed event
                kafkaTemplate.send("payment-completed", payment);
            } else {
                payment.setStatus("FAILED");

                // Send payment failed event
                kafkaTemplate.send("payment-failed", payment);
            }

            return paymentRepository.save(payment);
        });
    }

    public CompletableFuture<Payment> processPaymentFallback(Payment payment, Exception ex) {
        logger.error("Fallback executed for processPayment. Error: {}", ex.getMessage());

        // Create a record of the attempted payment
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus("PENDING_CONFIRMATION");
        payment.setTransactionId(UUID.randomUUID().toString());

        Payment savedPayment = paymentRepository.save(payment);

        // Send to a special topic for manual processing
        kafkaTemplate.send("payment-fallback", savedPayment);

        return CompletableFuture.completedFuture(savedPayment);
    }

    private boolean processWithPaymentGateway(Payment payment) {
        // In a real application, this would integrate with a payment gateway
        // For this example, we'll simulate a successful payment most of the time
        return Math.random() > 0.2; // 80% success rate
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "refundPaymentFallback")
    @Retry(name = "orderService", fallbackMethod = "refundPaymentFallback")
    @Transactional
    public Payment refundPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(payment -> {
                    if ("COMPLETED".equals(payment.getStatus())) {
                        // Process refund with payment gateway
                        boolean refundSuccessful = processRefundWithPaymentGateway(payment);

                        if (refundSuccessful) {
                            payment.setStatus("REFUNDED");

                            // Update order status
                            orderClient.updateOrderStatus(payment.getOrderId(), "REFUNDED");

                            // Send payment refunded event
                            kafkaTemplate.send("payment-refunded", payment);
                        } else {
                            // Send refund failed event
                            kafkaTemplate.send("refund-failed", payment);
                        }

                        return paymentRepository.save(payment);
                    } else {
                        throw new RuntimeException("Payment cannot be refunded. Current status: " + payment.getStatus());
                    }
                })
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    public Payment refundPaymentFallback(Long paymentId, Exception ex) {
        logger.error("Fallback executed for refundPayment. PaymentId: {}, Error: {}", paymentId, ex.getMessage());

        return paymentRepository.findById(paymentId)
                .map(payment -> {
                    payment.setStatus("REFUND_PENDING");
                    Payment savedPayment = paymentRepository.save(payment);

                    // Send to a special topic for manual processing
                    kafkaTemplate.send("refund-fallback", savedPayment);

                    return savedPayment;
                })
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    private boolean processRefundWithPaymentGateway(Payment payment) {
        // In a real application, this would integrate with a payment gateway
        // For this example, we'll simulate a successful refund most of the time
        return Math.random() > 0.1; // 90% success rate
    }
}
