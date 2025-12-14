package tqs.blacktie.e2e;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tqs.blacktie.dto.SetRoleRequest;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.SignUpResponse;
import tqs.blacktie.entity.Product;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Product Image E2E Tests")
class ProductImageE2ETest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private Long registerUser(String name, String password) {
        String email = uniqueEmail();
        SignUpRequest req = new SignUpRequest(name, email, password);
        ResponseEntity<SignUpResponse> res =
                restTemplate.postForEntity(url("/api/auth/register"), req, SignUpResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(CREATED);
        assertThat(res.getBody()).isNotNull();
        return res.getBody().getId();
    }

    private void promoteToOwner(Long userId) {
        HttpEntity<SetRoleRequest> setRoleEntity = new HttpEntity<>(new SetRoleRequest("owner"));
        ResponseEntity<?> roleRes = restTemplate.exchange(
                url("/api/users/" + userId + "/role"),
                HttpMethod.POST,
                setRoleEntity,
                Object.class
        );
        assertThat(roleRes.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("Should create product with image successfully")
    void whenCreateProductWithImage_thenSuccess() {
        Long ownerId = registerUser("Owner", "ownerpass");
        promoteToOwner(ownerId);

        // Create product with image
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Smoking with Photo");
        body.add("description", "Black smoking with image");
        body.add("price", "150.0");
        
        // Create a fake image
        byte[] imageContent = "fake image content".getBytes();
        ByteArrayResource imageResource = new ByteArrayResource(imageContent) {
            @Override
            public String getFilename() {
                return "suit.jpg";
            }
        };
        body.add("image", imageResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-User-Id", ownerId.toString());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Product> response = restTemplate.postForEntity(
                url("/api/products/with-image"),
                request,
                Product.class
        );

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        Product product = response.getBody();
        assertThat(product.getName()).isEqualTo("Smoking with Photo");
        assertThat(product.getDescription()).isEqualTo("Black smoking with image");
        assertThat(product.getPrice()).isEqualTo(150.0);
        assertThat(product.getImageUrl()).isNotNull();
        assertThat(product.getImageUrl()).contains("/api/products/images/");
    }

    @Test
    @DisplayName("Should create product without image successfully")
    void whenCreateProductWithoutImage_thenSuccess() {
        Long ownerId = registerUser("Owner", "ownerpass");
        promoteToOwner(ownerId);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Tuxedo without Photo");
        body.add("description", "Navy tuxedo");
        body.add("price", "120.0");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-User-Id", ownerId.toString());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Product> response = restTemplate.postForEntity(
                url("/api/products/with-image"),
                request,
                Product.class
        );

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        Product product = response.getBody();
        assertThat(product.getName()).isEqualTo("Tuxedo without Photo");
        assertThat(product.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should retrieve uploaded image successfully")
    void whenGetUploadedImage_thenSuccess() {
        Long ownerId = registerUser("Owner", "ownerpass");
        promoteToOwner(ownerId);

        // Create product with image
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Suit with Image");
        body.add("description", "Gray suit");
        body.add("price", "100.0");
        
        byte[] imageContent = "test image bytes".getBytes();
        ByteArrayResource imageResource = new ByteArrayResource(imageContent) {
            @Override
            public String getFilename() {
                return "test.jpg";
            }
        };
        body.add("image", imageResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-User-Id", ownerId.toString());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Product> createResponse = restTemplate.postForEntity(
                url("/api/products/with-image"),
                request,
                Product.class
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        String imageUrl = createResponse.getBody().getImageUrl();
        assertThat(imageUrl).isNotNull();

        // Now retrieve the image
        ResponseEntity<byte[]> imageResponse = restTemplate.getForEntity(
                url(imageUrl),
                byte[].class
        );

        assertThat(imageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(imageResponse.getBody()).isNotNull();
        assertThat(imageResponse.getBody().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should return 404 for non-existent image")
    void whenGetNonExistentImage_thenNotFound() {
        assertThatThrownBy(() -> restTemplate.getForEntity(
                url("/api/products/images/non-existent-image.jpg"),
                byte[].class
        )).isInstanceOf(HttpClientErrorException.class)
          .hasMessageContaining("404");
    }

    @Test
    @DisplayName("Should fail when non-owner tries to create product with image")
    void whenNonOwnerCreatesProductWithImage_thenForbidden() {
        Long renterId = registerUser("Renter", "renterpass");
        // Don't promote to owner

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", "Unauthorized Product");
        body.add("description", "This should fail");
        body.add("price", "100.0");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-User-Id", renterId.toString());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        assertThatThrownBy(() -> restTemplate.postForEntity(
                url("/api/products/with-image"),
                request,
                Product.class
        )).isInstanceOf(HttpClientErrorException.class)
          .hasMessageContaining("403");
    }

    @Test
    @DisplayName("Should create product using JSON endpoint without image")
    void whenCreateProductUsingJsonEndpoint_thenSuccess() {
        Long ownerId = registerUser("Owner", "ownerpass");
        promoteToOwner(ownerId);

        Map<String, Object> productBody = new HashMap<>();
        productBody.put("name", "Classic Suit");
        productBody.put("description", "Traditional black suit");
        productBody.put("price", 90.0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", ownerId.toString());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(productBody, headers);

        ResponseEntity<Product> response = restTemplate.postForEntity(
                url("/api/products"),
                request,
                Product.class
        );

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        assertThat(response.getBody()).isNotNull();
        Product product = response.getBody();
        assertThat(product.getName()).isEqualTo("Classic Suit");
        assertThat(product.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("Should support multiple image formats")
    void whenUploadDifferentImageFormats_thenSuccess() {
        Long ownerId = registerUser("Owner", "ownerpass");
        promoteToOwner(ownerId);

        String[] filenames = {"suit.jpg", "dress.png", "tuxedo.jpeg"};
        
        for (String filename : filenames) {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("name", "Product with " + filename);
            body.add("description", "Testing " + filename);
            body.add("price", "100.0");
            
            byte[] imageContent = ("content for " + filename).getBytes();
            ByteArrayResource imageResource = new ByteArrayResource(imageContent) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
            body.add("image", imageResource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-User-Id", ownerId.toString());

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Product> response = restTemplate.postForEntity(
                    url("/api/products/with-image"),
                    request,
                    Product.class
            );

            assertThat(response.getStatusCode()).isEqualTo(CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getImageUrl()).isNotNull();
        }
    }
}
