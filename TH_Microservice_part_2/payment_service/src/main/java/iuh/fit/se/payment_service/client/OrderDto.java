package iuh.fit.se.payment_service.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Long id;
    private Long customerId;
    private LocalDateTime orderDate;
    private String status;
    private List<OrderItemDto> items;
    private Double totalAmount;
}