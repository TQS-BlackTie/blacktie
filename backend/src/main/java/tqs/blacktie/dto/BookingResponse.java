package tqs.blacktie.dto;

import java.time.LocalDateTime;

public class BookingResponse {
    
    private Long id;
    private Long renterId;
    private String renterName;
    private Long productId;
    private String productName;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime bookingDate;
    private LocalDateTime returnDate;
    private Double totalPrice;
    private String status;

    public BookingResponse() {
    }

    public BookingResponse(Long id, Long renterId, String renterName, Long productId, String productName, 
                          Long ownerId, String ownerName, LocalDateTime bookingDate, LocalDateTime returnDate, 
                          Double totalPrice, String status) {
        this.id = id;
        this.renterId = renterId;
        this.renterName = renterName;
        this.productId = productId;
        this.productName = productName;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.bookingDate = bookingDate;
        this.returnDate = returnDate;
        this.totalPrice = totalPrice;
        this.status = status;
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

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
