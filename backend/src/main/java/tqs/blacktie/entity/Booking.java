package tqs.blacktie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tqs.blacktie.constants.BookingConstants;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {
    
    public static final String STATUS_PENDING_APPROVAL = BookingConstants.STATUS_PENDING_APPROVAL;
    public static final String STATUS_APPROVED = BookingConstants.STATUS_APPROVED;
    public static final String STATUS_REJECTED = BookingConstants.STATUS_REJECTED;
    public static final String STATUS_PAID = BookingConstants.STATUS_PAID;
    public static final String STATUS_COMPLETED = BookingConstants.STATUS_COMPLETED;
    public static final String STATUS_CANCELLED = BookingConstants.STATUS_CANCELLED;
    
    public static final String DELIVERY_PICKUP = BookingConstants.DELIVERY_PICKUP;
    public static final String DELIVERY_SHIPPING = BookingConstants.DELIVERY_SHIPPING;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User renter;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private LocalDateTime bookingDate;
    private LocalDateTime returnDate;
    private Double totalPrice;

    @Column(nullable = false)
    private String status = STATUS_PENDING_APPROVAL;

    private String deliveryMethod; // PICKUP or SHIPPING
    private String deliveryCode; // Generated after payment if SHIPPING
    private String pickupLocation; // Address/instructions if PICKUP
    private String rejectionReason; // Reason if rejected by owner
    
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    
    // Deposit-related fields
    private Double depositAmount; // Amount of deposit required
    private Boolean depositRequested = false; // Whether deposit has been requested
    private String depositReason; // Reason for requesting deposit
    private LocalDateTime depositRequestedAt; // When deposit was requested
    private Boolean depositPaid = false; // Whether deposit has been paid
    private LocalDateTime depositPaidAt; // When deposit was paid
    
    public Booking(User renter, Product product, LocalDateTime bookingDate, LocalDateTime returnDate, Double totalPrice) {
        this.renter = renter;
        this.product = product;
        this.bookingDate = bookingDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
        this.status = STATUS_PENDING_APPROVAL;
    }
}
