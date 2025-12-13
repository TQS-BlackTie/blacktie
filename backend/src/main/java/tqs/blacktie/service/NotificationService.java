package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import tqs.blacktie.dto.NotificationResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Notification;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.NotificationRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.List;

@Service
public class NotificationService {

    private static final String USER_NOT_FOUND_MSG = "User not found with id: ";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void createNewBookingNotification(User owner, Booking booking) {
        String message = String.format("New booking for your product '%s' by %s", 
            booking.getProduct().getName(), 
            booking.getRenter().getName());
        
        Notification notification = new Notification(owner, Notification.TYPE_NEW_BOOKING, message, booking);
        notificationRepository.save(notification);
    }

    public void createBookingCancelledByRenterNotification(User owner, Booking booking) {
        String message = String.format("%s cancelled their booking for '%s'", 
            booking.getRenter().getName(), 
            booking.getProduct().getName());
        
        Notification notification = new Notification(owner, Notification.TYPE_BOOKING_CANCELLED_BY_RENTER, message, booking);
        notificationRepository.save(notification);
    }

    public void createBookingCancelledByOwnerNotification(User renter, Booking booking) {
        String message = String.format("Your booking for '%s' was cancelled by the owner", 
            booking.getProduct().getName());
        
        Notification notification = new Notification(renter, Notification.TYPE_BOOKING_CANCELLED_BY_OWNER, message, booking);
        notificationRepository.save(notification);
    }

    public void createBookingApprovedNotification(User renter, Booking booking) {
        String message = String.format("Your booking for '%s' has been approved! You can now proceed with payment.", 
            booking.getProduct().getName());
        
        Notification notification = new Notification(renter, Notification.TYPE_BOOKING_APPROVED, message, booking);
        notificationRepository.save(notification);
    }

    public void createBookingRejectedNotification(User renter, Booking booking, String reason) {
        String message = String.format("Your booking for '%s' was rejected.", 
            booking.getProduct().getName());
        if (reason != null && !reason.isEmpty()) {
            message += " Reason: " + reason;
        }
        
        Notification notification = new Notification(renter, Notification.TYPE_BOOKING_REJECTED, message, booking);
        notificationRepository.save(notification);
    }

    public void createPaymentReceivedNotification(User owner, Booking booking) {
        String message = String.format("Payment received for booking of '%s' by %s", 
            booking.getProduct().getName(),
            booking.getRenter().getName());
        
        Notification notification = new Notification(owner, Notification.TYPE_PAYMENT_RECEIVED, message, booking);
        notificationRepository.save(notification);
    }

    public List<NotificationResponse> getUserNotifications(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG + userId));
        
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        return notifications.stream()
            .map(this::convertToResponse)
            .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG + userId));
        
        List<Notification> notifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        return notifications.stream()
            .map(this::convertToResponse)
            .toList();
    }

    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG + userId));
        
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
        
        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User is not authorized to modify this notification");
        }
        
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG + userId));
        
        List<Notification> unreadNotifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        unreadNotifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);
    }

    private NotificationResponse convertToResponse(Notification notification) {
        Long bookingId = notification.getBooking() != null ? notification.getBooking().getId() : null;
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getMessage(),
            bookingId,
            notification.getIsRead(),
            notification.getCreatedAt()
        );
    }
}
