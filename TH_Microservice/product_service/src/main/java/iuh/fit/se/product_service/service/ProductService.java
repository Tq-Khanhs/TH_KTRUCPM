package iuh.fit.se.product_service.service;


import iuh.fit.se.product_service.model.Product;
import iuh.fit.se.product_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Optional<Product> updateProduct(Long id, Product productDetails) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    existingProduct.setName(productDetails.getName());
                    existingProduct.setDescription(productDetails.getDescription());
                    existingProduct.setPrice(productDetails.getPrice());
                    existingProduct.setStockQuantity(productDetails.getStockQuantity());
                    return productRepository.save(existingProduct);
                });
    }

    public boolean deleteProduct(Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    productRepository.delete(product);
                    return true;
                })
                .orElse(false);
    }

    public boolean updateStock(Long productId, int quantity) {
        return productRepository.findById(productId)
                .map(product -> {
                    int newQuantity = product.getStockQuantity() - quantity;
                    if (newQuantity >= 0) {
                        product.setStockQuantity(newQuantity);
                        productRepository.save(product);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}