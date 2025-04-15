package iuh.fit.se.inventory_service.controller;

import iuh.fit.se.inventory_service.model.InVentoryItem;
import iuh.fit.se.inventory_service.model.InventoryTransaction;
import iuh.fit.se.inventory_service.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<List<InVentoryItem>> getAllInventoryItems() {
        return ResponseEntity.ok(inventoryService.getAllInventoryItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InVentoryItem> getInventoryItemById(@PathVariable Long id) {
        return inventoryService.getInventoryItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<InVentoryItem> getInventoryItemByProductId(@PathVariable Long productId) {
        return inventoryService.getInventoryItemByProductId(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/update")
    public ResponseEntity<InVentoryItem> updateInventory(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            String type = request.get("type").toString();
            String reason = request.get("reason").toString();
            Long referenceId = Long.valueOf(request.get("referenceId").toString());

            InVentoryItem updatedItem = inventoryService.updateInventory(productId, quantity, type, reason, referenceId);
            return ResponseEntity.ok(updatedItem);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @GetMapping("/transactions/product/{productId}")
    public ResponseEntity<List<InventoryTransaction>> getTransactionsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getTransactionsByProductId(productId));
    }

    @GetMapping("/transactions/reference/{referenceId}")
    public ResponseEntity<List<InventoryTransaction>> getTransactionsByReferenceId(@PathVariable Long referenceId) {
        return ResponseEntity.ok(inventoryService.getTransactionsByReferenceId(referenceId));
    }
}
