package tqs.blacktie.entity;

import jakarta.persistence.*;
import tqs.blacktie.constants.BookingConstants;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
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

    public Booking() {
        this.status = STATUS_PENDING_APPROVAL;
    }
    
    public Booking(User renter, Product product, LocalDateTime bookingDate, LocalDateTime returnDate, Double totalPrice) {
        this.renter = renter;
        this.product = product;
        this.bookingDate = bookingDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
        this.status = STATUS_PENDING_APPROVAL;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public User getRenter() {
        return renter;
    }
    public void setRenter(User renter) {
        this.renter = renter;
    }
    public Product getProduct() {
        return product;
    }
    public void setProduct(Product product) {
        this.product = product;
    }
    public LocalDateTime getBookingDate() {
        return bookingDate;
    }
    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }
    public LocalDateTime getReturnDate() {
        return returnDate;
    }
    public void setReturnDate(LocalDateTime returnDate) {
        this.returnDate = returnDate;
    }
    public Double getTotalPrice() {
        return totalPrice;
    }
    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDeliveryMethod() {
        return deliveryMethod;
    }
    
    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }
    
    public String getDeliveryCode() {
        return deliveryCode;
    }
    
    public void setDeliveryCode(String deliveryCode) {
        this.deliveryCode = deliveryCode;
    }
    
    public String getPickupLocation() {
        return pickupLocation;
    }
    
    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }
    
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
