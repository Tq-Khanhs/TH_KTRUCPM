package iuh.fit.se.payment_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${app.order-service.url}")
public interface OrderClient {

    @GetMapping("/orders/{id}")
    ResponseEntity<OrderDto> getOrderById(@PathVariable Long id);

    @PutMapping("/orders/{id}/status")
    ResponseEntity<OrderDto> updateOrderStatus(@PathVariable Long id, @RequestParam String status);
}