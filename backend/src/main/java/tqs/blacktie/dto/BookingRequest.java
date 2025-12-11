package tqs.blacktie.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class BookingRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Booking date is required")
    private LocalDateTime bookingDate;
    
    @NotNull(message = "Return date is required")
    private LocalDateTime returnDate;

    public BookingRequest() {
    }

    public BookingRequest(Long productId, LocalDateTime bookingDate, LocalDateTime returnDate) {
        this.productId = productId;
        this.bookingDate = bookingDate;
        this.returnDate = returnDate;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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
}
