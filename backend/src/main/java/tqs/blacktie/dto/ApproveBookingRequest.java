package tqs.blacktie.dto;

import jakarta.validation.constraints.NotBlank;

public class ApproveBookingRequest {
    
    @NotBlank(message = "Delivery method is required")
    private String deliveryMethod; // PICKUP or SHIPPING
    
    private String pickupLocation; // Required if deliveryMethod is PICKUP

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }
}
