package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import tqs.blacktie.entity.Product;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<Product> getAvailableProducts(String name, Double maxPrice, Long requesterId) {
        var requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + requesterId));

        boolean isOwner = "owner".equalsIgnoreCase(requester.getRole());
        String trimmedName = name != null && !name.isBlank() ? name : null;

        if (isOwner) {
            if (trimmedName != null && maxPrice != null) {
                return productRepository
                    .findByOwnerAndAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual(
                        requester, trimmedName, maxPrice);
            }
            if (trimmedName != null) {
                return productRepository
                    .findByOwnerAndAvailableTrueAndNameContainingIgnoreCase(requester, trimmedName);
            }
            if (maxPrice != null) {
                return productRepository
                    .findByOwnerAndAvailableTrueAndPriceLessThanEqual(requester, maxPrice);
            }
            return productRepository.findByOwnerAndAvailableTrue(requester);
        }

        if (trimmedName != null && maxPrice != null) {
            return productRepository
                .findByAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual(trimmedName, maxPrice);
        }

        if (trimmedName != null) {
            return productRepository.findByAvailableTrueAndNameContainingIgnoreCase(trimmedName);
        }

        if (maxPrice != null) {
            return productRepository.findByAvailableTrueAndPriceLessThanEqual(maxPrice);
        }

        return productRepository.findByAvailableTrue();
    }

    public Product createProduct(Product product, Long ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner id is required");
        }
        var owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + ownerId));
        if (owner.getRole() == null || !"owner".equalsIgnoreCase(owner.getRole())) {
            throw new IllegalStateException("Only owners can create products");
        }
        product.setOwner(owner);
        if (product.getAvailable() == null) {
            product.setAvailable(true);
        }
        return productRepository.save(product);
    }
}
