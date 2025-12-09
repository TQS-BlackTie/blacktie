package tqs.blacktie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    
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
    private String status = "ACTIVE"; // ACTIVE, COMPLETED, CANCELLED

    public Booking() {
        this.status = "ACTIVE";
    }
    
    public Booking(User renter, Product product, LocalDateTime bookingDate, LocalDateTime returnDate, Double totalPrice) {
        this.renter = renter;
        this.product = product;
        this.bookingDate = bookingDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
        this.status = "ACTIVE";
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
}
