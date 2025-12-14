package tqs.blacktie.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Booking date is required")
    private LocalDateTime bookingDate;
    
    @NotNull(message = "Return date is required")
    private LocalDateTime returnDate;
}
