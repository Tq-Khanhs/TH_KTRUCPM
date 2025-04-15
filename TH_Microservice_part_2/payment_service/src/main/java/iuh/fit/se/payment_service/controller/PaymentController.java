package iuh.fit.se.payment_service.controller;

import iuh.fit.se.payment_service.model.Payment;
import iuh.fit.se.payment_service.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Payment>> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<Payment> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Payment>> processPayment(@RequestBody Payment payment) {
        return paymentService.processPayment(payment)
                .thenApply(result -> ResponseEntity.status(HttpStatus.CREATED).body(result))
                .exceptionally(ex -> {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long id) {
        try {
            Payment refundedPayment = paymentService.refundPayment(id);
            return ResponseEntity.ok(refundedPayment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
