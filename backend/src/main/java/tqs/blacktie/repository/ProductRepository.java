package tqs.blacktie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tqs.blacktie.entity.Product;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Todos os disponíveis
    List<Product> findByAvailableTrue();

    // Disponíveis com nome contendo texto (case-insensitive)
    List<Product> findByAvailableTrueAndNameContainingIgnoreCase(String name);

    // Disponíveis com preço <= maxPrice
    List<Product> findByAvailableTrueAndPriceLessThanEqual(Double maxPrice);

    // Disponíveis com nome contendo texto E preço <= maxPrice
    List<Product> findByAvailableTrueAndNameContainingIgnoreCaseAndPriceLessThanEqual(
        String name,
        Double maxPrice
    );
}
