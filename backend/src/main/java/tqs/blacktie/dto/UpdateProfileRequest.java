package tqs.blacktie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Pattern(regexp = "^\\+?[0-9\\s-()]{9,20}$", message = "Invalid phone number format")
    private String phone;

    @Size(max = 200, message = "Address must not exceed 200 characters")
    private String address;

    @Size(max = 500, message = "Business info must not exceed 500 characters")
    private String businessInfo;
}
