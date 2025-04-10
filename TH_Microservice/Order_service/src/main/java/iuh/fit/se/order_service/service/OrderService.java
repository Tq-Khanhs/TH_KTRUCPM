package iuh.fit.se.order_service.service;

import com.example.orderservice.client.CustomerServiceClient;
import com.example.orderservice.client.ProductServiceClient;
import com.example.orderservice.dto.OrderItemRequest;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.dto.ProductDto;
import com.example.orderservice.dto.StockUpdateRequest;
import com.example.orderservice.dto.StatusUpdateRequest;
import com.example.orderservice.messaging.OrderEventPublisher;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderItem;
import com.example.orderservice.repository.OrderRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final OrderEventPublisher orderEventPublisher;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
    }

    @Transactional
    public Order createOrder(OrderRequest orderRequest) {
        // Verify customer exists
        try {
            customerServiceClient.getCustomer(orderRequest.getCustomerId());
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new EntityNotFoundException("Customer not found with id: " + orderRequest.getCustomerId());
            }
            throw e;
        }

        // Verify products and calculate total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = orderRequest.getItems().stream()
                .map(this::createOrderItemFromRequest)
                .collect(Collectors.toList());

        // Create order
        Order order = new Order();
        order.setCustomerId(orderRequest.getCustomerId());
        order.setTotalAmount(totalAmount);
        order.setStatus("pending");

        // Add items to order
        orderItems.forEach(order::addItem);

        // Calculate total amount
        for (OrderItem item : orderItems) {
            totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setTotalAmount(totalAmount);

        // Save order
        Order savedOrder = orderRepository.save(order);

        // Update product stock
        for (OrderItemRequest itemRequest : orderRequest.getItems()) {
            productServiceClient.updateStock(
                    itemRequest.getProductId(),
                    new StockUpdateRequest(itemRequest.getQuantity())
            );
        }

        // Publish order created event
        orderEventPublisher.publishOrderCreated(savedOrder);

        return savedOrder;
    }

    private OrderItem createOrderItemFromRequest(OrderItemRequest itemRequest) {
        ProductDto product = productServiceClient.getProduct(itemRequest.getProductId());
        
        if (product.getStockQuantity() < itemRequest.getQuantity()) {
            throw new IllegalStateException(
                    "Insufficient stock for product: " + product.getName() +
                    ", available: " + product.getStockQuantity() +
                    ", requested: " + itemRequest.getQuantity()
            );
        }
        
        OrderItem orderItem = new OrderItem();
        orderItem.setProductId(itemRequest.getProductId());
        orderItem.setQuantity(itemRequest.getQuantity());
        orderItem.setPrice(product.getPrice());
        
        return orderItem;
    }

    @Transactional
    public Order updateOrderStatus(Long id, StatusUpdateRequest request) {
        Order order = getOrderById(id);
        
        String status = request.getStatus();
        if (!isValidStatus(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        // Publish order status updated event
        orderEventPublisher.publishOrderStatusUpdated(updatedOrder);
        
        return updatedOrder;
    }

    private boolean isValidStatus(String status) {
        return List.of("pending", "processing", "shipped", "delivered", "cancelled").contains(status);
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = getOrderById(id);
        
        if (!canCancel(order)) {
            throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
        }
        
        // Restore product stock
        for (OrderItem item : order.getItems()) {
            productServiceClient.updateStock(
                    item.getProductId(),
                    new StockUpdateRequest(-item.getQuantity()) // Negative to add back to stock
            );
        }
        
        order.setStatus("cancelled");
        Order cancelledOrder = orderRepository.save(order);
        
        // Publish order cancelled event
        orderEventPublisher.publishOrderCancelled(cancelledOrder);
    }

    private boolean canCancel(Order order) {
        return order.getStatus().equals("pending") || order.getStatus().equals("processing");
    }
}
