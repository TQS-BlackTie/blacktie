package tqs.blacktie.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tqs.blacktie.entity.Product;
import tqs.blacktie.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public List<Product> getProducts(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) Double maxPrice,
			@RequestHeader("X-User-Id") Long userId
	) {
		return productService.getAvailableProducts(name, maxPrice, userId);
	}

	@PostMapping
	public ResponseEntity<?> createProduct(
		@RequestBody Product product,
		@RequestHeader("X-User-Id") Long userId
	) {
		try {
			Product created = productService.createProduct(product, userId);
			return ResponseEntity.status(HttpStatus.CREATED).body(created);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}
}
