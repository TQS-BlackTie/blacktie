package tqs.blacktie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateUserStatusRequest {
    
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(active|suspended|banned)$", message = "Status must be 'active', 'suspended', or 'banned'")
    private String status;

    public UpdateUserStatusRequest() {
    }

    public UpdateUserStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
