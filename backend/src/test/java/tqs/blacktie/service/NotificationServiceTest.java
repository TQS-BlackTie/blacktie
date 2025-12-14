package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.dto.NotificationResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Notification;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.NotificationRepository;
import tqs.blacktie.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User owner;
    private User renter;
    private Product product;
    private Booking booking;
    private Notification notification;

    @BeforeEach
    void setUp() {
        owner = new User("Owner Name", "owner@test.com", "password", "owner");
        owner.setId(1L);

        renter = new User("Renter Name", "renter@test.com", "password", "renter");
        renter.setId(2L);

        product = new Product();
        product.setId(1L);
        product.setName("Black Suit");
        product.setOwner(owner);

        booking = new Booking(renter, product, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking.setId(1L);

        notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Test notification", booking);
        notification.setId(1L);
    }

    @Test
    void testCreateNewBookingNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createNewBookingNotification(owner, booking);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateBookingCancelledByRenterNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createBookingCancelledByRenterNotification(owner, booking);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateBookingCancelledByOwnerNotification() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createBookingCancelledByOwnerNotification(renter, booking);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testGetUserNotifications() {
        Notification notification2 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Second notification",
                booking);
        notification2.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(owner))
                .thenReturn(Arrays.asList(notification2, notification));

        List<NotificationResponse> result = notificationService.getUserNotifications(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(2L);
        assertThat(result.get(1).getId()).isEqualTo(1L);
        verify(notificationRepository).findByUserOrderByCreatedAtDesc(owner);
    }

    @Test
    void testGetUserNotifications_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getUserNotifications(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void testGetUnreadNotifications() {
        notification.setIsRead(false);
        Notification notification2 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Unread notification",
                booking);
        notification2.setId(2L);
        notification2.setIsRead(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(owner, false))
                .thenReturn(Arrays.asList(notification2, notification));

        List<NotificationResponse> result = notificationService.getUnreadNotifications(1L);

        assertThat(result)
                .hasSize(2)
                .allMatch(n -> !n.getIsRead());
    }

    @Test
    void testGetUnreadCount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(notificationRepository.countByUserAndIsRead(owner, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1L);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).countByUserAndIsRead(owner, false);
    }

    @Test
    void testMarkAsRead() {
        notification.setIsRead(false);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.markAsRead(1L, 1L);

        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void testMarkAsRead_NotificationNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notification not found");
    }

    @Test
    void testMarkAsRead_UnauthorizedUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void testMarkAllAsRead() {
        Notification notification2 = new Notification(owner, Notification.TYPE_NEW_BOOKING, "Unread 2", booking);
        notification2.setIsRead(false);
        notification.setIsRead(false);

        List<Notification> unreadNotifications = Arrays.asList(notification, notification2);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(owner, false))
                .thenReturn(unreadNotifications);
        when(notificationRepository.saveAll(unreadNotifications)).thenReturn(unreadNotifications);

        notificationService.markAllAsRead(1L);

        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification2.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(unreadNotifications);
    }

    @Test
    void testMarkAllAsRead_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAllAsRead(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void testCreateDepositRefundedNotification() {
        booking.setDepositAmount(50.0);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createDepositRefundedNotification(renter, booking, 50.0);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testCreateDepositRefundedNotification_WithNullAmount() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.createDepositRefundedNotification(renter, booking, null);

        verify(notificationRepository).save(any(Notification.class));
    }
}
