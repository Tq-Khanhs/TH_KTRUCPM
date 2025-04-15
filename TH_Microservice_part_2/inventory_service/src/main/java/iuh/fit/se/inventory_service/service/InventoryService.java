package iuh.fit.se.inventory_service.service;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import iuh.fit.se.inventory_service.client.ProductClient;
import iuh.fit.se.inventory_service.model.InVentoryItem;
import iuh.fit.se.inventory_service.model.InventoryTransaction;
import iuh.fit.se.inventory_service.repository.InventoryItemRepository;
import iuh.fit.se.inventory_service.repository.InventoryTransactionRepository;
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

@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public InventoryService(InventoryItemRepository inventoryItemRepository,
                            InventoryTransactionRepository transactionRepository,
                            ProductClient productClient,
                            KafkaTemplate<String, Object> kafkaTemplate) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.transactionRepository = transactionRepository;
        this.productClient = productClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<InVentoryItem> getAllInventoryItems() {
        return inventoryItemRepository.findAll();
    }

    public Optional<InVentoryItem> getInventoryItemById(Long id) {
        return inventoryItemRepository.findById(id);
    }

    public Optional<InVentoryItem> getInventoryItemByProductId(Long productId) {
        return inventoryItemRepository.findByProductId(productId);
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "updateInventoryFallback")
    @Retry(name = "productService", fallbackMethod = "updateInventoryFallback")
    @Transactional
    public InVentoryItem updateInventory(Long productId, Integer quantity, String type, String reason, Long referenceId) {
        // Get or create inventory item
        InVentoryItem inventoryItem = inventoryItemRepository.findByProductId(productId)
                .orElseGet(() -> {
                    InVentoryItem newItem = new InVentoryItem();
                    newItem.setProductId(productId);
                    newItem.setQuantity(0);
                    newItem.setLocation("DEFAULT");
                    newItem.setLastUpdated(LocalDateTime.now());
                    return inventoryItemRepository.save(newItem);
                });

        // Update inventory quantity
        if ("IN".equals(type)) {
            inventoryItem.setQuantity(inventoryItem.getQuantity() + quantity);
        } else if ("OUT".equals(type)) {
            if (inventoryItem.getQuantity() < quantity) {
                throw new RuntimeException("Insufficient inventory for product: " + productId);
            }
            inventoryItem.setQuantity(inventoryItem.getQuantity() - quantity);
        }

        inventoryItem.setLastUpdated(LocalDateTime.now());
        InVentoryItem updatedItem = inventoryItemRepository.save(inventoryItem);

        // Create transaction record
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductId(productId);
        transaction.setQuantity(quantity);
        transaction.setType(type);
        transaction.setReason(reason);
        transaction.setReferenceId(referenceId);
        transaction.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Update product stock in Product Service
        productClient.updateStock(productId, "IN".equals(type) ? quantity : -quantity);

        // Send inventory updated event
        kafkaTemplate.send("inventory-updated", Map.of(
                "productId", productId,
                "quantity", inventoryItem.getQuantity(),
                "type", type,
                "reason", reason,
                "referenceId", referenceId
        ));

        return updatedItem;
    }

    public InVentoryItem updateInventoryFallback(Long productId, Integer quantity, String type, String reason, Long referenceId, Exception ex) {
        logger.error("Fallback executed for updateInventory. ProductId: {}, Error: {}", productId, ex.getMessage());

        // Get or create inventory item without updating the product service
        InVentoryItem inventoryItem = inventoryItemRepository.findByProductId(productId)
                .orElseGet(() -> {
                    InVentoryItem newItem = new InVentoryItem();
                    newItem.setProductId(productId);
                    newItem.setQuantity(0);
                    newItem.setLocation("DEFAULT");
                    newItem.setLastUpdated(LocalDateTime.now());
                    return inventoryItemRepository.save(newItem);
                });

        // Create transaction record for manual processing
        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProductId(productId);
        transaction.setQuantity(quantity);
        transaction.setType(type + "_PENDING");
        transaction.setReason(reason);
        transaction.setReferenceId(referenceId);
        transaction.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(transaction);

        // Send to a special topic for manual processing
        kafkaTemplate.send("inventory-update-fallback", Map.of(
                "productId", productId,
                "quantity", quantity,
                "type", type,
                "reason", reason,
                "referenceId", referenceId
        ));

        return inventoryItem;
    }

    public List<InventoryTransaction> getTransactionsByProductId(Long productId) {
        return transactionRepository.findByProductId(productId);
    }

    public List<InventoryTransaction> getTransactionsByReferenceId(Long referenceId) {
        return transactionRepository.findByReferenceId(referenceId);
    }

    @KafkaListener(topics = "order-created", groupId = "inventory-group")
    public void handleOrderCreated(Map<String, Object> orderData) {
        try {
            Long orderId = Long.valueOf(orderData.get("id").toString());
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");

            for (Map<String, Object> item : items) {
                Long productId = Long.valueOf(item.get("productId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                // Reserve inventory for the order
                updateInventory(productId, quantity, "OUT", "ORDER", orderId);
            }
        } catch (Exception e) {
            logger.error("Error processing order-created event", e);
        }
    }

    @KafkaListener(topics = "order-cancelled", groupId = "inventory-group")
    public void handleOrderCancelled(Map<String, Object> orderData) {
        try {
            Long orderId = Long.valueOf(orderData.get("id").toString());
            List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");

            for (Map<String, Object> item : items) {
                Long productId = Long.valueOf(item.get("productId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                // Return inventory for the cancelled order
                updateInventory(productId, quantity, "IN", "ORDER_CANCELLED", orderId);
            }
        } catch (Exception e) {
            logger.error("Error processing order-cancelled event", e);
        }
    }
}