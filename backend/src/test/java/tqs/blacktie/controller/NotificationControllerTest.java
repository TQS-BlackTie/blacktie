package tqs.blacktie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tqs.blacktie.dto.NotificationResponse;
import tqs.blacktie.entity.Notification;
import tqs.blacktie.service.NotificationService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private NotificationResponse notification1;
    private NotificationResponse notification2;

    @BeforeEach
    void setUp() {
        notification1 = new NotificationResponse(
            1L,
            Notification.TYPE_NEW_BOOKING,
            "New booking for your product",
            1L,
            false,
            LocalDateTime.now()
        );

        notification2 = new NotificationResponse(
            2L,
            Notification.TYPE_BOOKING_CANCELLED_BY_RENTER,
            "Booking was cancelled",
            2L,
            true,
            LocalDateTime.now()
        );
    }

    @Nested
    class GetUserNotificationsTests {
        @Test
        void shouldReturnAllNotifications() {
            List<NotificationResponse> notifications = Arrays.asList(notification1, notification2);
            when(notificationService.getUserNotifications(1L)).thenReturn(notifications);

            ResponseEntity<List<NotificationResponse>> response = notificationController.getUserNotifications(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getId()).isEqualTo(1L);
            verify(notificationService, times(1)).getUserNotifications(1L);
        }

        @Test
        void shouldReturnEmptyListWhenNoNotifications() {
            when(notificationService.getUserNotifications(1L)).thenReturn(List.of());

            ResponseEntity<List<NotificationResponse>> response = notificationController.getUserNotifications(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    class GetUnreadNotificationsTests {
        @Test
        void shouldReturnOnlyUnreadNotifications() {
            List<NotificationResponse> unreadNotifications = List.of(notification1);
            when(notificationService.getUnreadNotifications(1L)).thenReturn(unreadNotifications);

            ResponseEntity<List<NotificationResponse>> response = notificationController.getUnreadNotifications(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getIsRead()).isFalse();
            verify(notificationService, times(1)).getUnreadNotifications(1L);
        }
    }

    @Nested
    class GetUnreadCountTests {
        @Test
        void shouldReturnCorrectUnreadCount() {
            when(notificationService.getUnreadCount(1L)).thenReturn(3L);

            ResponseEntity<Map<String, Long>> response = notificationController.getUnreadCount(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("count", 3L);
            verify(notificationService, times(1)).getUnreadCount(1L);
        }

        @Test
        void shouldReturnZeroWhenNoUnreadNotifications() {
            when(notificationService.getUnreadCount(1L)).thenReturn(0L);

            ResponseEntity<Map<String, Long>> response = notificationController.getUnreadCount(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsEntry("count", 0L);
        }
    }

    @Nested
    class MarkAsReadTests {
        @Test
        void shouldMarkNotificationAsRead() {
            doNothing().when(notificationService).markAsRead(1L, 1L);

            ResponseEntity<Void> response = notificationController.markAsRead(1L, 1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(notificationService, times(1)).markAsRead(1L, 1L);
        }
    }

    @Nested
    class MarkAllAsReadTests {
        @Test
        void shouldMarkAllNotificationsAsRead() {
            doNothing().when(notificationService).markAllAsRead(1L);

            ResponseEntity<Void> response = notificationController.markAllAsRead(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(notificationService, times(1)).markAllAsRead(1L);
        }
    }
}
