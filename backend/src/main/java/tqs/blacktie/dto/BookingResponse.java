package tqs.blacktie.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
    private String deliveryMethod;
    private String deliveryCode;
    private String pickupLocation;
    private String rejectionReason;
    private LocalDateTime approvedAt;
    private LocalDateTime paidAt;
    private Double depositAmount;
    private Boolean depositRequested;
    private String depositReason;
    private LocalDateTime depositRequestedAt;
    private Boolean depositPaid;
    private LocalDateTime depositPaidAt;
    
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
}
