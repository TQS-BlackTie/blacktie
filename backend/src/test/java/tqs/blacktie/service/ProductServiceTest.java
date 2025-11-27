package tqs.blacktie.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tqs.blacktie.entity.Product;
import tqs.blacktie.repository.ProductRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository productRepository = Mockito.mock(ProductRepository.class);
    private final ProductService productService = new ProductService(productRepository);

    @Test
    void whenNoFilters_thenReturnsAllAvailable() {
        Product p = new Product("Smoking", "Desc", 80.0);
        p.setAvailable(true);

        when(productRepository.findByAvailableTrue()).thenReturn(List.of(p));

        List<Product> result = productService.getAvailableProducts(null, null);

        assertThat(result).hasSize(1).first().isEqualTo(p);
        verify(productRepository).findByAvailableTrue();
    }

    @Test
    void whenNameFilter_thenUsesNameQuery() {
        when(productRepository.findByAvailableTrueAndNameContainingIgnoreCase("smoking"))
                .thenReturn(List.of());

        productService.getAvailableProducts("smoking", null);

        verify(productRepository)
                .findByAvailableTrueAndNameContainingIgnoreCase("smoking");
    }

    @Test
    void whenMaxPriceFilter_thenUsesPriceQuery() {
        when(productRepository.findByAvailableTrueAndPriceLessThanEqual(100.0))
                .thenReturn(List.of());

        productService.getAvailableProducts(null, 100.0);

        verify(productRepository)
                .findByAvailableTrueAndPriceLessThanEqual(100.0);
    }

    @Test
    void whenNameAndMaxPriceFilter_thenUsesCombinedQuery() {
        when(productRepository
                .findByAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual("smoking", 120.0))
                .thenReturn(List.of());

        productService.getAvailableProducts("smoking", 120.0);

        verify(productRepository)
                .findByAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual("smoking", 120.0);
    }

    @Test
    void createProductDefaultsAvailableToTrueWhenNull() {
        Product toSave = new Product("Smoking", "Desc", 80.0);
        toSave.setAvailable(null);

        Product saved = new Product("Smoking", "Desc", 80.0);
        saved.setAvailable(true);

        when(productRepository.save(Mockito.any(Product.class))).thenReturn(saved);

        Product result = productService.createProduct(toSave);

        assertThat(result.getAvailable()).isTrue();
    }
}
