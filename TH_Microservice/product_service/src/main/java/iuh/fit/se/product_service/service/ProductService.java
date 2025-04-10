package iuh.fit.se.product_service.service;

import com.example.productservice.dto.StockUpdateRequest;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        Product product = getProductById(id);
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    @Transactional
    public Product updateStock(Long id, StockUpdateRequest request) {
        Product product = getProductById(id);
        
        int currentStock = product.getStockQuantity();
        int newStock = currentStock - request.getQuantity();
        
        if (newStock < 0) {
            throw new IllegalStateException("Insufficient stock for product: " + id);
        }
        
        product.setStockQuantity(newStock);
        return productRepository.save(product);
    }
}
