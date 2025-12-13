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
import tqs.blacktie.dto.SetRoleRequest;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.SignUpResponse;
import tqs.blacktie.entity.Product;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingCreationE2ETest {

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
        SignUpRequest req = new SignUpRequest(name, email, password);
        ResponseEntity<SignUpResponse> res =
            restTemplate.postForEntity(url("/api/auth/register"), req, SignUpResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(CREATED);
        assertThat(res.getBody()).isNotNull();
        return res.getBody().getId();
    }

    private Long createOwnedProduct(Long ownerId, String name, double price) {
        Map<String, Object> productBody = new HashMap<>();
        productBody.put("name", name);
        productBody.put("description", "Desc");
        productBody.put("price", price);
        HttpHeaders productHeaders = new HttpHeaders();
        productHeaders.setContentType(MediaType.APPLICATION_JSON);
        productHeaders.set("X-User-Id", ownerId.toString());
        HttpEntity<Map<String, Object>> productRequest = new HttpEntity<>(productBody, productHeaders);
        ResponseEntity<Product> productRes = restTemplate.postForEntity(
            url("/api/products"), productRequest, Product.class);
        assertThat(productRes.getStatusCode()).isEqualTo(CREATED);
        assertThat(productRes.getBody()).isNotNull();
        return productRes.getBody().getId();
    }

    @Test
    void renterCanCreateBookingAndFailsOnOverlaps() {
        Long ownerId = registerUser("Owner", "ownerpass");
        Long renterId = registerUser("Renter", "renterpass");

        // Promote owner
        HttpEntity<SetRoleRequest> setRoleEntity = new HttpEntity<>(new SetRoleRequest("owner"));
        ResponseEntity<?> roleRes = restTemplate.exchange(
            url("/api/users/" + ownerId + "/role"),
            HttpMethod.POST,
            setRoleEntity,
            Object.class
        );
        assertThat(roleRes.getStatusCode().is2xxSuccessful()).isTrue();

        Long productId = createOwnedProduct(ownerId, "Overlap Test Suit", 100.0);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withNano(0);
        LocalDateTime end = start.plusDays(3);
        BookingRequest firstBooking = new BookingRequest(productId, start, end);
        HttpHeaders bookingHeaders = new HttpHeaders();
        bookingHeaders.setContentType(MediaType.APPLICATION_JSON);
        bookingHeaders.set("X-User-Id", renterId.toString());
        HttpEntity<BookingRequest> bookingEntity = new HttpEntity<>(firstBooking, bookingHeaders);

        ResponseEntity<BookingResponse> firstRes = restTemplate.postForEntity(
            url("/api/bookings"), bookingEntity, BookingResponse.class);
        assertThat(firstRes.getStatusCode()).isEqualTo(CREATED);
        assertThat(firstRes.getBody()).isNotNull();
        assertThat(firstRes.getBody().getProductId()).isEqualTo(productId);

        // Approve the first booking so it blocks overlaps (only APPROVED/PAID bookings block)
        Long bookingId = firstRes.getBody().getId();
        HttpHeaders ownerHeaders = new HttpHeaders();
        ownerHeaders.setContentType(MediaType.APPLICATION_JSON);
        ownerHeaders.set("X-User-Id", ownerId.toString());
        
        String approvalJson = "{\"deliveryMethod\":\"PICKUP\",\"pickupLocation\":\"123 Main St\"}";
        HttpEntity<String> approvalEntity = new HttpEntity<>(approvalJson, ownerHeaders);
        restTemplate.exchange(
            url("/api/bookings/" + bookingId + "/approve"),
            HttpMethod.PUT,
            approvalEntity,
            BookingResponse.class
        );

        // Attempt overlapping booking - should now fail because first booking is APPROVED
        BookingRequest overlapping = new BookingRequest(productId, start.plusDays(1), end.plusDays(1));
        HttpEntity<BookingRequest> overlapEntity = new HttpEntity<>(overlapping, bookingHeaders);
        
        assertThatThrownBy(() -> 
            restTemplate.postForEntity(url("/api/bookings"), overlapEntity, String.class)
        )
        .isInstanceOf(HttpClientErrorException.class)
        .satisfies(e -> {
            HttpClientErrorException ex = (HttpClientErrorException) e;
            assertThat(ex.getStatusCode().value()).isIn(400, 409);
        });
    }

    @Test
    void bookingFailsForPastDate() {
        Long ownerId = registerUser("Owner2", "ownerpass2");
        Long renterId = registerUser("Renter2", "renterpass2");

        HttpEntity<SetRoleRequest> setRoleEntity = new HttpEntity<>(new SetRoleRequest("owner"));
        restTemplate.exchange(url("/api/users/" + ownerId + "/role"), HttpMethod.POST, setRoleEntity, Object.class);
        Long productId = createOwnedProduct(ownerId, "Past Date Suit", 90.0);

        LocalDateTime start = LocalDateTime.now().minusDays(1).withNano(0);
        LocalDateTime end = start.plusDays(2);
        BookingRequest badBooking = new BookingRequest(productId, start, end);
        HttpHeaders bookingHeaders = new HttpHeaders();
        bookingHeaders.setContentType(MediaType.APPLICATION_JSON);
        bookingHeaders.set("X-User-Id", renterId.toString());
        HttpEntity<BookingRequest> bookingEntity = new HttpEntity<>(badBooking, bookingHeaders);

        assertThatThrownBy(() -> 
            restTemplate.postForEntity(url("/api/bookings"), bookingEntity, String.class)
        )
        .isInstanceOf(HttpClientErrorException.BadRequest.class)
        .hasMessageContaining("400");
    }
}
