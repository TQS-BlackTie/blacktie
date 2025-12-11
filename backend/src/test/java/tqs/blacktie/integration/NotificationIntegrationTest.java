package tqs.blacktie.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tqs.blacktie.dto.NotificationResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Notification;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.NotificationRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;
import tqs.blacktie.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BookingRepository bookingRepository;

    private User owner;
    private User renter;
    private Product product;
    private Booking booking;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User("Owner Name", "owner@test.com", "password", "owner");
        owner = userRepository.save(owner);

        renter = new User("Renter Name", "renter@test.com", "password", "renter");
        renter = userRepository.save(renter);

        product = new Product();
        product.setName("Black Suit");
        product.setDescription("Elegant black suit");
        product.setPrice(50.0);
        product.setAvailable(true);
        product.setOwner(owner);
        product = productRepository.save(product);

        booking = new Booking(renter, product, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking = bookingRepository.save(booking);
    }

    @Test
    void testCreateNewBookingNotificationIntegration() {
        notificationService.createNewBookingNotification(owner, booking);

        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(owner);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(Notification.TYPE_NEW_BOOKING);
        assertThat(notifications.get(0).getMessage()).contains(product.getName());
        assertThat(notifications.get(0).getMessage()).contains(renter.getName());
        assertThat(notifications.get(0).getIsRead()).isFalse();
    }

    @Test
    void testCreateBookingCancelledByRenterNotificationIntegration() {
        notificationService.createBookingCancelledByRenterNotification(owner, booking);

        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(owner);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(Notification.TYPE_BOOKING_CANCELLED_BY_RENTER);
        assertThat(notifications.get(0).getMessage()).contains(renter.getName());
        assertThat(notifications.get(0).getMessage()).contains("cancelled");
    }

    @Test
    void testCreateBookingCancelledByOwnerNotificationIntegration() {
        notificationService.createBookingCancelledByOwnerNotification(renter, booking);

        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(renter);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(Notification.TYPE_BOOKING_CANCELLED_BY_OWNER);
        assertThat(notifications.get(0).getMessage()).contains(product.getName());
        assertThat(notifications.get(0).getMessage()).contains("cancelled by the owner");
    }

    @Test
    void testGetUserNotificationsIntegration() {
        notificationService.createNewBookingNotification(owner, booking);
        notificationService.createNewBookingNotification(owner, booking);

        List<NotificationResponse> notifications = notificationService.getUserNotifications(owner.getId());

        assertThat(notifications).hasSize(2);
        assertThat(notifications).allMatch(n -> !n.getIsRead());
    }

    @Test
    void testGetUnreadNotificationsIntegration() {
        Notification notification1 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Message 1", booking);
        notification1.setIsRead(false);
        notificationRepository.save(notification1);

        Notification notification2 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Message 2", booking);
        notification2.setIsRead(true);
        notificationRepository.save(notification2);

        List<NotificationResponse> unreadNotifications = notificationService.getUnreadNotifications(owner.getId());

        assertThat(unreadNotifications).hasSize(1);
        assertThat(unreadNotifications.get(0).getIsRead()).isFalse();
    }

    @Test
    void testGetUnreadCountIntegration() {
        for (int i = 0; i < 5; i++) {
            Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Message " + i, booking);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }

        long count = notificationService.getUnreadCount(owner.getId());

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void testMarkAsReadIntegration() {
        Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Test message", booking);
        notification.setIsRead(false);
        notification = notificationRepository.save(notification);

        notificationService.markAsRead(notification.getId(), owner.getId());

        Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertThat(updated.getIsRead()).isTrue();
    }

    @Test
    void testMarkAsReadUnauthorizedIntegration() {
        Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Test message", booking);
        Notification savedNotification = notificationRepository.save(notification);

        assertThatThrownBy(() -> notificationService.markAsRead(savedNotification.getId(), renter.getId()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not authorized");
    }

    @Test
    void testMarkAllAsReadIntegration() {
        for (int i = 0; i < 3; i++) {
            Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Message " + i, booking);
            notification.setIsRead(false);
            notificationRepository.save(notification);
        }

        notificationService.markAllAsRead(owner.getId());

        long unreadCount = notificationRepository.countByUserAndIsRead(owner, false);
        assertThat(unreadCount).isZero();

        List<Notification> allNotifications = notificationRepository.findByUserOrderByCreatedAtDesc(owner);
        assertThat(allNotifications).allMatch(Notification::getIsRead);
    }

    @Test
    void testNotificationOrderingIntegration() throws InterruptedException {
        Notification notification1 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "First", booking);
        notificationRepository.save(notification1);

        Thread.sleep(100); // Ensure different timestamps

        Notification notification2 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Second", booking);
        notificationRepository.save(notification2);

        List<NotificationResponse> notifications = notificationService.getUserNotifications(owner.getId());

        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getMessage()).isEqualTo("Second"); // Most recent first
        assertThat(notifications.get(1).getMessage()).isEqualTo("First");
    }
}
