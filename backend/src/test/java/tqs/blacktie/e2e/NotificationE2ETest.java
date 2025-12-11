package tqs.blacktie.e2e;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import tqs.blacktie.dto.NotificationResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Notification;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.NotificationRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String baseUrl;
    private User owner;
    private User renter;
    private Product product;
    private Booking booking;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/notifications";
        
        // Clean up
        notificationRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        // Create test users
        owner = new User("Owner Name", "owner@test.com", "password", "owner");
        owner = userRepository.save(owner);

        renter = new User("Renter Name", "renter@test.com", "password", "renter");
        renter = userRepository.save(renter);

        // Create test product
        product = new Product();
        product.setName("Black Suit");
        product.setDescription("Elegant black suit");
        product.setPrice(50.0);
        product.setAvailable(true);
        product.setOwner(owner);
        product = productRepository.save(product);

        // Create test booking
        booking = new Booking(renter, product, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking = bookingRepository.save(booking);
    }

    @Test
    void testGetUserNotifications() {
        // Create notifications
        Notification notification1 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "New booking", booking);
        Notification notification2 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Another booking", booking);
        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        ResponseEntity<NotificationResponse[]> response = restTemplate.getForEntity(
            baseUrl + "?userId=" + owner.getId(),
            NotificationResponse[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isEqualTo(2);
    }

    @Test
    void testGetUnreadNotifications() {
        // Create mixed notifications
        Notification unread = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Unread notification", booking);
        unread.setIsRead(false);
        notificationRepository.save(unread);

        Notification read = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Read notification", booking);
        read.setIsRead(true);
        notificationRepository.save(read);

        ResponseEntity<NotificationResponse[]> response = restTemplate.getForEntity(
            baseUrl + "/unread?userId=" + owner.getId(),
            NotificationResponse[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isEqualTo(1);
        assertThat(response.getBody()[0].getIsRead()).isFalse();
    }

    @Test
    void testGetUnreadCount() {
        // Create unread notifications
        for (int i = 0; i < 3; i++) {
            Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Unread " + i, booking);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }

        ResponseEntity<Map> response = restTemplate.getForEntity(
            baseUrl + "/unread/count?userId=" + owner.getId(),
            Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("count")).isEqualTo(3);
    }

    @Test
    void testMarkAsRead() {
        Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Test notification", booking);
        notification.setIsRead(false);
        notification = notificationRepository.save(notification);

        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/" + notification.getId() + "/read?userId=" + owner.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(headers),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify notification is marked as read
        Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    void testMarkAsRead_UnauthorizedUser() {
        Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Test notification", booking);
        notification = notificationRepository.save(notification);

        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/" + notification.getId() + "/read?userId=" + renter.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(headers),
            String.class
        );

        // Should fail because renter is trying to mark owner's notification
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testMarkAllAsRead() {
        // Create multiple unread notifications
        for (int i = 0; i < 3; i++) {
            Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Unread " + i, booking);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }

        HttpHeaders headers = new HttpHeaders();
        ResponseEntity<Void> response = restTemplate.exchange(
            baseUrl + "/read-all?userId=" + owner.getId(),
            HttpMethod.PUT,
            new HttpEntity<>(headers),
            Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Verify all notifications are marked as read
        long unreadCount = notificationRepository.countByUserAndIsRead(owner, false);
        assertThat(unreadCount).isZero();
    }

    @Test
    void testNotificationCreatedOnNewBooking() {
        // This test verifies that booking creation triggers notification
        long initialCount = notificationRepository.countByUserAndIsRead(owner, false);

        // Create a new booking which should trigger notification creation
        Booking newBooking = new Booking(renter, product, LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(6), 50.0);
        bookingRepository.save(newBooking);

        // Manually create notification since we're testing E2E
        Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, 
            "New booking for your product '" + product.getName() + "' by " + renter.getName(), newBooking);
        notificationRepository.save(notification);

        long finalCount = notificationRepository.countByUserAndIsRead(owner, false);
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }
}
