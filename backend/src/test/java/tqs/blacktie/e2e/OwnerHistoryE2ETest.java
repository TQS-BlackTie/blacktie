package tqs.blacktie.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.SignUpResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OwnerHistoryE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private BookingRepository bookingRepository;

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
        productRequest.put("name", "OwnerProd");
        productRequest.put("description", "desc");
        productRequest.put("available", true);
        productRequest.put("price", 10.0);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(productRequest, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                url("/api/products"),
                HttpMethod.POST,
                entity,
                Map.class
        );

        return ((Number) response.getBody().get("id")).longValue();
    }

    private Long createBooking(Long renterId, Long productId, LocalDateTime start, LocalDateTime end) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-User-Id", renterId.toString());

        BookingRequest request = new BookingRequest(productId, start, end);

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
    void testOwnerHistoryContainsCompletedAndCancelled() {
        // register users (use passwords >= 8 chars to pass validation)
        Long ownerId = registerUser("OwnerX", "StrongPass1");
        Long renterId = registerUser("RenterX", "StrongPass2");

        setUserRole(ownerId, "owner");
        setUserRole(renterId, "renter");

        // create product
        Long productId = createProduct(ownerId);

        // create two bookings
        LocalDateTime now = LocalDateTime.now();
        Long b1 = createBooking(renterId, productId, now.plusDays(1), now.plusDays(2));
        Long b2 = createBooking(renterId, productId, now.plusDays(3), now.plusDays(4));

        // cancel second booking as owner
        HttpHeaders cancelHeaders = new HttpHeaders();
        cancelHeaders.set("X-User-Id", ownerId.toString());
        HttpEntity<Void> cancelEntity = new HttpEntity<>(cancelHeaders);
        restTemplate.exchange(url("/api/bookings/" + b2), HttpMethod.DELETE, cancelEntity, Void.class);

        // mark first booking as COMPLETED directly via repository to simulate past booking
        Booking booking = bookingRepository.findById(b1).orElseThrow();
        booking.setStatus("COMPLETED");
        bookingRepository.save(booking);

        // request owner history
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", ownerId.toString());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        @SuppressWarnings("unchecked")
        ResponseEntity<java.util.List<BookingResponse>> resp = (ResponseEntity<java.util.List<BookingResponse>>) (ResponseEntity<?>) restTemplate.exchange(
                url("/api/bookings/owner/history"),
                HttpMethod.GET,
                entity,
                new org.springframework.core.ParameterizedTypeReference<java.util.List<BookingResponse>>() {}
        );

        assertThat(resp.getStatusCode()).isEqualTo(OK);
        assertThat(resp.getBody()).isNotNull();
        // should contain at least the cancelled and completed we set
        boolean hasCompleted = resp.getBody().stream().anyMatch(b -> "COMPLETED".equals(b.getStatus()));
        boolean hasCancelled = resp.getBody().stream().anyMatch(b -> "CANCELLED".equals(b.getStatus()));

        assertThat(hasCompleted).isTrue();
        assertThat(hasCancelled).isTrue();
    }
}
