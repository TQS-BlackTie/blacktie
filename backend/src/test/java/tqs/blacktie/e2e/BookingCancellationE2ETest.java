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
import static org.springframework.http.HttpStatus.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BookingCancellationE2ETest {

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

    @Test
    void renterCanCreateAndCancelBooking() {
        Long ownerId = registerUser("Owner", "ownerpass");
        Long renterId = registerUser("Renter", "renterpass");

        // Promote owner
        HttpEntity<SetRoleRequest> setRoleEntity = new HttpEntity<>(new SetRoleRequest("owner"));
        ResponseEntity<?> roleRes = restTemplate.exchange(
            url("/api/users/" + ownerId + "/role"),
            HttpMethod.PUT,
            setRoleEntity,
            Object.class
        );
        assertThat(roleRes.getStatusCode()).isEqualTo(OK);

        // Owner creates product
        Map<String, Object> productBody = new HashMap<>();
        productBody.put("name", "Test Suit");
        productBody.put("description", "Black tie suit");
        productBody.put("price", 120.0);
        HttpHeaders productHeaders = new HttpHeaders();
        productHeaders.setContentType(MediaType.APPLICATION_JSON);
        productHeaders.set("X-User-Id", ownerId.toString());
        HttpEntity<Map<String, Object>> productRequest = new HttpEntity<>(productBody, productHeaders);
        ResponseEntity<Product> productRes = restTemplate.postForEntity(
            url("/api/products"),
            productRequest,
            Product.class
        );
        assertThat(productRes.getStatusCode()).isEqualTo(CREATED);
        assertThat(productRes.getBody()).isNotNull();
        Long productId = productRes.getBody().getId();

        // Renter creates booking
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        LocalDateTime end = start.plusDays(2);
        BookingRequest bookingRequest = new BookingRequest(productId, start, end);
        HttpHeaders bookingHeaders = new HttpHeaders();
        bookingHeaders.setContentType(MediaType.APPLICATION_JSON);
        bookingHeaders.set("X-User-Id", renterId.toString());
        HttpEntity<BookingRequest> bookingEntity = new HttpEntity<>(bookingRequest, bookingHeaders);
        ResponseEntity<BookingResponse> bookingRes = restTemplate.postForEntity(
            url("/api/bookings"),
            bookingEntity,
            BookingResponse.class
        );
        assertThat(bookingRes.getStatusCode()).isEqualTo(CREATED);
        assertThat(bookingRes.getBody()).isNotNull();
        Long bookingId = bookingRes.getBody().getId();

        // Renter cancels booking
        HttpHeaders cancelHeaders = new HttpHeaders();
        cancelHeaders.set("X-User-Id", renterId.toString());
        ResponseEntity<Void> cancelRes = restTemplate.exchange(
            url("/api/bookings/" + bookingId),
            HttpMethod.DELETE,
            new HttpEntity<>(cancelHeaders),
            Void.class
        );
        assertThat(cancelRes.getStatusCode()).isEqualTo(NO_CONTENT);

        // Second cancel attempt should return 404 (already removed)
        assertThatThrownBy(() ->
            restTemplate.exchange(
                url("/api/bookings/" + bookingId),
                HttpMethod.DELETE,
                new HttpEntity<>(cancelHeaders),
                Void.class
            )
        )
        .isInstanceOf(HttpClientErrorException.NotFound.class)
        .hasMessageContaining("404");
    }
}
