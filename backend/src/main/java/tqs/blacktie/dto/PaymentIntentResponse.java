package tqs.blacktie.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    
    private String clientSecret;
    private String paymentIntentId;
    private Long amount;
    private String currency;
}
