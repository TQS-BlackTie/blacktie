package tqs.blacktie.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestDepositRequest {
    
    @NotNull(message = "Deposit amount is required")
    @Positive(message = "Deposit amount must be positive")
    private Double depositAmount;
    
    @NotNull(message = "Reason for deposit is required")
    private String reason;
}
