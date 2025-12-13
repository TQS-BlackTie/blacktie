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

    public UserResponse(Long id, String name, String email, String role, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }
}
