package tqs.blacktie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    
    public static final String TYPE_NEW_BOOKING = "NEW_BOOKING";
    public static final String TYPE_BOOKING_CANCELLED_BY_RENTER = "BOOKING_CANCELLED_BY_RENTER";
    public static final String TYPE_BOOKING_CANCELLED_BY_OWNER = "BOOKING_CANCELLED_BY_OWNER";
    
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

    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(User user, String type, String message, Booking booking) {
        this.user = user;
        this.type = type;
        this.message = message;
        this.booking = booking;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
