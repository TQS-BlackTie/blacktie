package tqs.blacktie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para definir o papel do utilizador na primeira utilização
 */
public class SetRoleRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(renter|owner)$", message = "Role must be 'renter' or 'owner'")
    private String role;

    public SetRoleRequest() {
    }

    public SetRoleRequest(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
