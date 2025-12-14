package tqs.blacktie.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String phone;
    private String address;
    private String businessInfo;
    private String createdAt;

    // Reputation Stats
    private Double averageRating;
    private Integer totalReviews;
    private Double renterAverageRating;
    private Integer renterReviewCount;
    private Double ownerAverageRating;
    private Integer ownerReviewCount;

    public UserResponse(Long id, String name, String email, String role, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UserResponse(Long id, String name, String email, String role, String phone, String address, String businessInfo, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.phone = phone;
        this.address = address;
        this.businessInfo = businessInfo;
        this.createdAt = createdAt;
    }
}
