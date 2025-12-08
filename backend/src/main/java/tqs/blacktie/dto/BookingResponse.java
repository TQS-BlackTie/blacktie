package tqs.blacktie.dto;

import java.time.LocalDateTime;

public class BookingResponse {
    
    private Long id;
    private Long renterId;
    private String renterName;
    private Long productId;
    private String productName;
    private LocalDateTime bookingDate;
    private LocalDateTime returnDate;
    private Double totalPrice;

    public BookingResponse() {
    }

    public BookingResponse(Long id, Long renterId, String renterName, Long productId, String productName, 
                          LocalDateTime bookingDate, LocalDateTime returnDate, Double totalPrice) {
        this.id = id;
        this.renterId = renterId;
        this.renterName = renterName;
        this.productId = productId;
        this.productName = productName;
        this.bookingDate = bookingDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRenterId() {
        return renterId;
    }

    public void setRenterId(Long renterId) {
        this.renterId = renterId;
    }

    public String getRenterName() {
        return renterName;
    }

    public void setRenterName(String renterName) {
        this.renterName = renterName;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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
}
