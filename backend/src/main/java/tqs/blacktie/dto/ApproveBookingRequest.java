package tqs.blacktie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApproveBookingRequest {
    
    @NotBlank(message = "Delivery method is required")
    private String deliveryMethod; // PICKUP or SHIPPING
    
    private String pickupLocation; // Required if deliveryMethod is PICKUP
}
