package tqs.blacktie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    public static final String TYPE_NEW_BOOKING = "NEW_BOOKING";
    public static final String TYPE_BOOKING_CANCELLED_BY_RENTER = "BOOKING_CANCELLED_BY_RENTER";
    public static final String TYPE_BOOKING_CANCELLED_BY_OWNER = "BOOKING_CANCELLED_BY_OWNER";
    public static final String TYPE_BOOKING_APPROVED = "BOOKING_APPROVED";
    public static final String TYPE_BOOKING_REJECTED = "BOOKING_REJECTED";
    public static final String TYPE_PAYMENT_RECEIVED = "PAYMENT_RECEIVED";
    public static final String TYPE_ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED";
    public static final String TYPE_ACCOUNT_BANNED = "ACCOUNT_BANNED";
    public static final String TYPE_ACCOUNT_REACTIVATED = "ACCOUNT_REACTIVATED";
    public static final String TYPE_BOOKING_CANCELLED_BY_ADMIN = "BOOKING_CANCELLED_BY_ADMIN";
    public static final String TYPE_PRODUCT_DELETED_BY_ADMIN = "PRODUCT_DELETED_BY_ADMIN";
    public static final String TYPE_DEPOSIT_REQUESTED = "DEPOSIT_REQUESTED";
    public static final String TYPE_DEPOSIT_PAID = "DEPOSIT_PAID";
    public static final String TYPE_DEPOSIT_REFUNDED = "DEPOSIT_REFUNDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // The user who receives the notification

    @Column(nullable = false)
    private String type; // NEW_BOOKING, BOOKING_CANCELLED_BY_RENTER, BOOKING_CANCELLED_BY_OWNER

    @Column(nullable = false, length = 500)
    private String message;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking; // Related booking

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Notification(User user, String type, String message, Booking booking) {
        this.user = user;
        this.type = type;
        this.message = message;
        this.booking = booking;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }
}
