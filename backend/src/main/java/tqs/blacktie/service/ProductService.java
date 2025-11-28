package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import tqs.blacktie.entity.Product;
import tqs.blacktie.repository.ProductRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAvailableProducts(String name, Double maxPrice) {
        if (name != null && !name.isBlank() && maxPrice != null) {
            return productRepository
                .findByAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual(name, maxPrice);
        }

        if (name != null && !name.isBlank()) {
            return productRepository.findByAvailableTrueAndNameContainingIgnoreCase(name);
        }

        if (maxPrice != null) {
            return productRepository.findByAvailableTrueAndPriceLessThanEqual(maxPrice);
        }

        return productRepository.findByAvailableTrue();
    }

    public Product createProduct(Product product) {
        if (product.getAvailable() == null) {
            product.setAvailable(true);
        }
        return productRepository.save(product);
    }
}

