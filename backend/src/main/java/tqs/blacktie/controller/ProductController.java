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
			@RequestParam(required = false) Double maxPrice
	) {
		return productService.getAvailableProducts(name, maxPrice);
	}

	@PostMapping
	public ResponseEntity<Product> createProduct(@RequestBody Product product) {
		Product created = productService.createProduct(product);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}
