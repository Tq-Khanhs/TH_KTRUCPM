package iuh.fit.se.inventory_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "${app.product-service.url}")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ResponseEntity<ProductDto> getProductById(@PathVariable Long id);

    @PutMapping("/products/{id}/stock")
    ResponseEntity<Void> updateStock(@PathVariable Long id, @RequestParam int quantity);
}
