package iuh.fit.se.inventory_service.repository;

import iuh.fit.se.inventory_service.model.InVentoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InVentoryItem, Long> {
    Optional<InVentoryItem> findByProductId(Long productId);
}
