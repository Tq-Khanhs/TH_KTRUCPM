package iuh.fit.se.inventory_service.repository;
import iuh.fit.se.inventory_service.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    List<InventoryTransaction> findByProductId(Long productId);
    List<InventoryTransaction> findByReferenceId(Long referenceId);
}
