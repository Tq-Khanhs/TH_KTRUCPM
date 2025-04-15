package iuh.fit.se.shipping_service.repository;


import iuh.fit.se.shipping_service.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByOrderId(Long orderId);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
}