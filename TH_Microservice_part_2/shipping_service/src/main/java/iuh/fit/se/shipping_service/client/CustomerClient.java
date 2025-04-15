package iuh.fit.se.shipping_service.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "customer-service", url = "${app.customer-service.url}")
public interface CustomerClient {

    @GetMapping("/customers/{id}")
    ResponseEntity<CustomerDto> getCustomerById(@PathVariable Long id);
}
