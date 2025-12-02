package tqs.blacktie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    public UpdateProfileRequest() {
    }

    public UpdateProfileRequest(String name, String phone, String address, String businessInfo) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.businessInfo = businessInfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBusinessInfo() {
        return businessInfo;
    }

    public void setBusinessInfo(String businessInfo) {
        this.businessInfo = businessInfo;
    }
}
