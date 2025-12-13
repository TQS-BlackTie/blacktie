package tqs.blacktie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para definir o papel do utilizador na primeira utilização
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetRoleRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(renter|owner)$", message = "Role must be 'renter' or 'owner'")
    private String role;
}
