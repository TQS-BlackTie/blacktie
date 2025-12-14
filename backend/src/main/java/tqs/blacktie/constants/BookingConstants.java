package tqs.blacktie.constants;

public final class BookingConstants {
    
    // Status constants
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    
    // Delivery method constants
    public static final String DELIVERY_PICKUP = "PICKUP";
    public static final String DELIVERY_SHIPPING = "SHIPPING";
    
    private BookingConstants() {
        // Prevent instantiation
    }
}
