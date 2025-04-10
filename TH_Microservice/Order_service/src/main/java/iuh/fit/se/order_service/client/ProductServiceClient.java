package iuh.fit.se.order_service.client;

import com.example.orderservice.dto.ProductDto;
import com.example.orderservice.dto.StockUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{id}")
    ProductDto getProduct(@PathVariable("id") Long id);

    @PatchMapping("/products/{id}/stock")
    ProductDto updateStock(@PathVariable("id") Long id, @RequestBody StockUpdateRequest request);
}
