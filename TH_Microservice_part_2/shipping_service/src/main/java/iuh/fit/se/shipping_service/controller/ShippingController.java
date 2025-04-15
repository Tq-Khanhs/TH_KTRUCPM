package iuh.fit.se.shipping_service.controller;


import iuh.fit.se.shipping_service.model.Shipment;
import iuh.fit.se.shipping_service.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/shipments")
public class ShippingController {

    private final ShippingService shippingService;

    @Autowired
    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping
    public ResponseEntity<List<Shipment>> getAllShipments() {
        return ResponseEntity.ok(shippingService.getAllShipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shipment> getShipmentById(@PathVariable Long id) {
        return shippingService.getShipmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Shipment>> getShipmentsByOrderId(@PathVariable Long orderId) {
        List<Shipment> shipments = shippingService.getShipmentsByOrderId(orderId);
        return ResponseEntity.ok(shipments);
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<Shipment> getShipmentByTrackingNumber(@PathVariable String trackingNumber) {
        return shippingService.getShipmentByTrackingNumber(trackingNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<Shipment>> createShipment(@RequestBody Shipment shipment) {
        return shippingService.createShipment(shipment)
                .thenApply(result -> ResponseEntity.status(HttpStatus.CREATED).body(result))
                .exceptionally(ex -> {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Shipment> updateShipmentStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            Shipment updatedShipment = shippingService.updateShipmentStatus(id, status);
            return ResponseEntity.ok(updatedShipment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}