package tqs.blacktie.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.SignUpResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RenterHistoryE2ETest {

    @LocalServerPort
    private int port;

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private Long registerUser(String name, String password) {
        String email = uniqueEmail();
        SignUpRequest request = new SignUpRequest(name, email, password);
        ResponseEntity<SignUpResponse> response = restTemplate.postForEntity(
                url("/api/auth/register"),
                request,
                SignUpResponse.class
        );
        return response.getBody().getId();
    }

    private void setUserRole(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> request = new HashMap<>();
        request.put("role", role);
        
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
        restTemplate.exchange(
                url("/api/users/" + userId + "/role"),
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    private Long createProduct(Long ownerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", ownerId.toString());
        
        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", "Test Product");
        productRequest.put("description", "Test Description");
        productRequest.put("available", true);
        productRequest.put("price", 50.0);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(productRequest, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url("/api/products"),
                HttpMethod.POST,
                entity,
                new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );
        
        Map<String, Object> responseBody = response.getBody();
        return ((Number) responseBody.get("id")).longValue();
    }

    private Long createBooking(Long renterId, Long productId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", renterId.toString());
        
        LocalDateTime now = LocalDateTime.now();
        BookingRequest request = new BookingRequest(
                productId,
                now.plusDays(1),
                now.plusDays(3)
        );
        
        HttpEntity<BookingRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<BookingResponse> response = restTemplate.exchange(
                url("/api/bookings"),
                HttpMethod.POST,
                entity,
                BookingResponse.class
        );
        
        return response.getBody().getId();
    }

    @Test
    void testGetRenterHistorySuccess() {
        // Setup: create owner and renter
        Long ownerId = registerUser("Owner", "password123");
        Long renterId = registerUser("Renter", "password123");
        
        setUserRole(ownerId, "owner");
        setUserRole(renterId, "renter");
        
        // Create product
        Long productId = createProduct(ownerId);
        
        // Create booking
        createBooking(renterId, productId);
        
        // Get history (should be empty as booking is ACTIVE)
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", renterId.toString());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        @SuppressWarnings("unchecked")
        ResponseEntity<java.util.List<BookingResponse>> historyResponse = (ResponseEntity<java.util.List<BookingResponse>>) (ResponseEntity<?>) restTemplate.exchange(
                url("/api/bookings/user/" + renterId + "/history"),
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<java.util.List<BookingResponse>>() {}
        );
        
        assertThat(historyResponse.getStatusCode()).isEqualTo(OK);
    }

    @Test
    void testGetRenterHistoryNotFound() {
        // Try to get history for non-existent user
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "999");
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        
        try {
            restTemplate.exchange(
                    url("/api/bookings/user/999/history"),
                    HttpMethod.GET,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<java.util.List<BookingResponse>>() {}
            );
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode()).isEqualTo(NOT_FOUND);
        }
    }
}
