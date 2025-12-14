package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import tqs.blacktie.dto.LocationDTO;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final LocationService locationService;

    public ProductService(ProductRepository productRepository, UserRepository userRepository, LocationService locationService) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.locationService = locationService;
    }

    public List<Product> getAvailableProducts(String name, Double maxPrice, Long requesterId) {
        var requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + requesterId));

        boolean isOwner = User.ROLE_OWNER.equalsIgnoreCase(requester.getRole());
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
        if (owner.getRole() == null || !User.ROLE_OWNER.equalsIgnoreCase(owner.getRole())) {
            throw new IllegalStateException("Only owners can create products");
        }
        product.setOwner(owner);
        if (product.getAvailable() == null) {
            product.setAvailable(true);
        }
        
        // Geocode location if address is provided
        if (product.getAddress() != null && !product.getAddress().isBlank()) {
            LocationDTO location = locationService.geocodeAddress(
                product.getAddress(), 
                product.getCity(), 
                product.getPostalCode()
            );
            product.setLatitude(location.getLatitude());
            product.setLongitude(location.getLongitude());
            product.setCity(location.getCity());
            product.setPostalCode(location.getPostalCode());
        } else if (product.getLatitude() != null && product.getLongitude() != null) {
            // Reverse geocode if only coordinates are provided
            LocationDTO location = locationService.reverseGeocode(
                product.getLatitude(), 
                product.getLongitude()
            );
            product.setAddress(location.getAddress());
            product.setCity(location.getCity());
            product.setPostalCode(location.getPostalCode());
        }
        
        return productRepository.save(product);
    }

    public void deleteProduct(Long productId, Long userId) {
        if (productId == null) {
            throw new IllegalArgumentException("Product id is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }

        var product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
        
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Only the owner can delete their product
        if (!product.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("You can only delete your own products");
        }

        // Mark product as unavailable instead of deleting to preserve referential integrity
        product.setAvailable(false);
        productRepository.save(product);
    }
}
