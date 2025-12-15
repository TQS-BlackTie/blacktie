package tqs.blacktie.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository productRepository = Mockito.mock(ProductRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final LocationService locationService = Mockito.mock(LocationService.class);
    private final ProductService productService = new ProductService(productRepository, userRepository, locationService);

    @Test
    void whenNoFilters_thenReturnsAllAvailable() {
        Product p = new Product("Smoking", "Desc", 80.0);
        p.setAvailable(true);

        when(productRepository.findByAvailableTrue()).thenReturn(List.of(p));

        User requester = new User("Renter", "r@example.com", "pass", "renter");
        requester.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(requester));

        List<Product> result = productService.getAvailableProducts(null, null, 1L);

        assertThat(result).hasSize(1).first().isEqualTo(p);
        verify(productRepository).findByAvailableTrue();
    }

    @Test
    void whenNameFilter_thenUsesNameQuery() {
        when(productRepository.findByAvailableTrueAndNameContainingIgnoreCase("smoking"))
                .thenReturn(List.of());

        User requester = new User("Renter", "r@example.com", "pass", "renter");
        requester.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(requester));

        productService.getAvailableProducts("smoking", null, 1L);

        verify(productRepository)
                .findByAvailableTrueAndNameContainingIgnoreCase("smoking");
    }

    @Test
    void whenMaxPriceFilter_thenUsesPriceQuery() {
        when(productRepository.findByAvailableTrueAndPriceLessThanEqual(100.0))
                .thenReturn(List.of());

        User requester = new User("Renter", "r@example.com", "pass", "renter");
        requester.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(requester));

        productService.getAvailableProducts(null, 100.0, 1L);

        verify(productRepository)
                .findByAvailableTrueAndPriceLessThanEqual(100.0);
    }

    @Test
    void whenNameAndMaxPriceFilter_thenUsesCombinedQuery() {
        when(productRepository
                .findByAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual("smoking", 120.0))
                .thenReturn(List.of());

        User requester = new User("Renter", "r@example.com", "pass", "renter");
        requester.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(requester));

        productService.getAvailableProducts("smoking", 120.0, 1L);

        verify(productRepository)
                .findByAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual("smoking", 120.0);
    }

    @Test
    void ownerShouldOnlySeeOwnProducts() {
        User owner = new User("Owner", "o@example.com", "pass", "owner");
        owner.setId(10L);
        when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(owner));

        productService.getAvailableProducts(null, null, 10L);

        verify(productRepository).findByOwnerAndAvailableTrue(owner);
    }

    @Test
    void shouldThrowWhenRequesterNotFound() {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> productService.getAvailableProducts(null, null, 99L));
    }

    @Test
    void shouldThrowWhenNonOwnerCreatesProduct() {
        User renter = new User("Renter", "r@example.com", "pass", "renter");
        renter.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(renter));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> productService.createProduct(new Product("Name", "Desc", 10.0), 1L));
    }

    @Test
    void shouldThrowWhenOwnerMissingOnCreate() {
        when(userRepository.findById(5L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> productService.createProduct(new Product("Name", "Desc", 10.0), 5L));
    }

    @Test
    void createProductDefaultsAvailableToTrueWhenNull() {
        Product toSave = new Product("Smoking", "Desc", 80.0);
        toSave.setAvailable(null);

        Product saved = new Product("Smoking", "Desc", 80.0);
        saved.setAvailable(true);
        User owner = new User("Owner", "o@example.com", "pass", "owner");
        owner.setId(10L);

        when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(owner));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        Product result = productService.createProduct(toSave, 10L);

        assertThat(result.getAvailable()).isTrue();
    }

    @Test
    void shouldMarkProductAsUnavailableWhenDeleted() {
        User owner = new User("Owner", "o@example.com", "pass", "owner");
        owner.setId(1L);
        
        Product product = new Product("Smoking", "Desc", 80.0);
        product.setId(1L);
        product.setOwner(owner);
        product.setAvailable(true);

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(owner));
        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        productService.deleteProduct(1L, 1L);

        verify(productRepository).save(product);
        assertThat(product.getAvailable()).isFalse();
    }

    @Test
    void shouldThrowWhenDeletingProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> productService.deleteProduct(999L, 1L));
    }

    @Test
    void shouldThrowWhenUserNotFoundOnDelete() {
        Product product = new Product("Smoking", "Desc", 80.0);
        product.setId(1L);
        User owner = new User("Owner", "o@example.com", "pass", "owner");
        owner.setId(1L);
        product.setOwner(owner);

        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));
        when(userRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> productService.deleteProduct(1L, 999L));
    }

    @Test
    void shouldThrowWhenNonOwnerTriesToDelete() {
        User owner = new User("Owner", "o@example.com", "pass", "owner");
        owner.setId(1L);
        
        User otherUser = new User("Other", "other@example.com", "pass", "owner");
        otherUser.setId(2L);

        Product product = new Product("Smoking", "Desc", 80.0);
        product.setId(1L);
        product.setOwner(owner);

        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));
        when(userRepository.findById(2L)).thenReturn(java.util.Optional.of(otherUser));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
            () -> productService.deleteProduct(1L, 2L));
    }

    @Test
    void shouldThrowWhenProductIdIsNull() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> productService.deleteProduct(null, 1L));
    }

    @Test
    void shouldThrowWhenUserIdIsNull() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> productService.deleteProduct(1L, null));
    }

    @Test
    void shouldFilterProductsByNameCaseInsensitive() {
        when(productRepository.findByAvailableTrueAndNameContainingIgnoreCase("SMOKING"))
                .thenReturn(List.of());

        User requester = new User("Renter", "r@example.com", "pass", "renter");
        requester.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(requester));

        productService.getAvailableProducts("SMOKING", null, 1L);

        verify(productRepository)
                .findByAvailableTrueAndNameContainingIgnoreCase("SMOKING");
    }

    @Test
    void shouldHandleZeroPriceFilter() {
        when(productRepository.findByAvailableTrueAndPriceLessThanEqual(0.0))
                .thenReturn(List.of());

        User requester = new User("Renter", "r@example.com", "pass", "renter");
        requester.setId(1L);
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(requester));

        productService.getAvailableProducts(null, 0.0, 1L);

        verify(productRepository)
                .findByAvailableTrueAndPriceLessThanEqual(0.0);
    }

    @Test
    void shouldCreateProductWithMinimalData() {
        Product minimal = new Product("Tux", "", 0.0);
        minimal.setAvailable(null);

        Product saved = new Product("Tux", "", 0.0);
        saved.setAvailable(true);
        saved.setId(1L);

        User owner = new User("Owner", "o@example.com", "pass", "owner");
        owner.setId(10L);

        when(userRepository.findById(10L)).thenReturn(java.util.Optional.of(owner));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        Product result = productService.createProduct(minimal, 10L);

        assertThat(result.getAvailable()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

}
