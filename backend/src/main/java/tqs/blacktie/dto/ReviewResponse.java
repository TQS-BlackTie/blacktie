package tqs.blacktie.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long bookingId;
    private Long productId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private String reviewType; // "OWNER" or "RENTER"
}
