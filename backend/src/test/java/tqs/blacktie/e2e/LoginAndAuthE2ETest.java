package tqs.blacktie.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import tqs.blacktie.dto.LoginRequest;
import tqs.blacktie.dto.LoginResponse;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.SignUpResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginAndAuthE2ETest {

    @LocalServerPort
    private int port;

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void userCanRegisterAndLogin() {
        String email = uniqueEmail();

        SignUpRequest register = new SignUpRequest("Alice", email, "password123");
        ResponseEntity<SignUpResponse> registerRes =
            restTemplate.postForEntity(url("/api/auth/register"), register, SignUpResponse.class);

        assertThat(registerRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerRes.getBody()).isNotNull();
        Long userId = registerRes.getBody().getId();
        assertThat(userId).isNotNull();

        LoginRequest login = new LoginRequest(email, "password123");
        ResponseEntity<LoginResponse> loginRes =
            restTemplate.postForEntity(url("/api/auth/login"), login, LoginResponse.class);

        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginRes.getBody()).isNotNull();
        assertThat(loginRes.getBody().getId()).isEqualTo(userId);
        assertThat(loginRes.getBody().getEmail()).isEqualTo(email);
        assertThat(loginRes.getBody().getRole()).isEqualTo("renter");
    }

    @Test
    void loginFailsWithWrongPassword() {
        String email = uniqueEmail();
        SignUpRequest register = new SignUpRequest("Bob", email, "correctpass");
        restTemplate.postForEntity(url("/api/auth/register"), register, SignUpResponse.class);

        LoginRequest badLogin = new LoginRequest(email, "wrongpass");
        
        assertThatThrownBy(() -> 
            restTemplate.postForEntity(url("/api/auth/login"), badLogin, String.class)
        )
        .isInstanceOf(HttpClientErrorException.Unauthorized.class)
        .hasMessageContaining("401");
    }
}
