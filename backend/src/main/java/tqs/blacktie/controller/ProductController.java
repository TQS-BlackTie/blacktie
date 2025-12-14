package tqs.blacktie.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tqs.blacktie.entity.Product;
import tqs.blacktie.service.ProductService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService productService;
	private static final String UPLOAD_DIR = "uploads/products/";

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

	@PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> createProductWithImage(
		@RequestParam("name") String name,
		@RequestParam("description") String description,
		@RequestParam("price") Double price,
		@RequestParam(value = "depositAmount", required = false) Double depositAmount,
		@RequestParam(value = "address", required = false) String address,
		@RequestParam(value = "city", required = false) String city,
		@RequestParam(value = "postalCode", required = false) String postalCode,
		@RequestParam(value = "latitude", required = false) Double latitude,
		@RequestParam(value = "longitude", required = false) Double longitude,
		@RequestParam(value = "image", required = false) MultipartFile image,
		@RequestHeader("X-User-Id") Long userId
	) {
		try {
			Product product = new Product();
			product.setName(name);
			product.setDescription(description);
			product.setPrice(price);
			product.setDepositAmount(depositAmount);
			product.setAddress(address);
			product.setCity(city);
			product.setPostalCode(postalCode);
			product.setLatitude(latitude);
			product.setLongitude(longitude);

			if (image != null && !image.isEmpty()) {
				String imageUrl = saveImage(image);
				product.setImageUrl(imageUrl);
			}

			Product created = productService.createProduct(product, userId);
			return ResponseEntity.status(HttpStatus.CREATED).body(created);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload image");
		}
	}

	private String saveImage(MultipartFile image) throws IOException {
		Path uploadPath = Paths.get(UPLOAD_DIR);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		String originalFilename = image.getOriginalFilename();
		String extension = originalFilename != null && originalFilename.contains(".") 
			? originalFilename.substring(originalFilename.lastIndexOf(".")) 
			: ".jpg";
		String filename = UUID.randomUUID().toString() + extension;
		
		Path filePath = uploadPath.resolve(filename);
		Files.copy(image.getInputStream(), filePath);
		
		return "/api/products/images/" + filename;
	}

	@GetMapping("/images/{filename}")
	public ResponseEntity<byte[]> getImage(@PathVariable String filename) {
		try {
			Path filePath = Paths.get(UPLOAD_DIR).resolve(filename);
			if (!Files.exists(filePath)) {
				return ResponseEntity.notFound().build();
			}
			
			byte[] imageBytes = Files.readAllBytes(filePath);
			String contentType = Files.probeContentType(filePath);
			if (contentType == null) {
				contentType = "image/jpeg";
			}
			
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.body(imageBytes);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DeleteMapping("/{productId}")
	public ResponseEntity<?> deleteProduct(
		@PathVariable Long productId,
		@RequestHeader("X-User-Id") Long userId
	) {
		try {
			productService.deleteProduct(productId, userId);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
		}
	}
}
