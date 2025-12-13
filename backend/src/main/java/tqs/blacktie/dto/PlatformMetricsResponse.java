package tqs.blacktie.dto;

public class PlatformMetricsResponse {
    
    private long totalUsers;
    private long totalOwners;
    private long totalRenters;
    private long totalProducts;
    private long availableProducts;
    private long totalBookings;
    private long activeBookings;
    private long completedBookings;
    private long cancelledBookings;
    private double totalRevenue;
    private double averageBookingValue;

    public PlatformMetricsResponse() {
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalOwners() {
        return totalOwners;
    }

    public void setTotalOwners(long totalOwners) {
        this.totalOwners = totalOwners;
    }

    public long getTotalRenters() {
        return totalRenters;
    }

    public void setTotalRenters(long totalRenters) {
        this.totalRenters = totalRenters;
    }

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getAvailableProducts() {
        return availableProducts;
    }

    public void setAvailableProducts(long availableProducts) {
        this.availableProducts = availableProducts;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public long getActiveBookings() {
        return activeBookings;
    }

    public void setActiveBookings(long activeBookings) {
        this.activeBookings = activeBookings;
    }

    public long getCompletedBookings() {
        return completedBookings;
    }

    public void setCompletedBookings(long completedBookings) {
        this.completedBookings = completedBookings;
    }

    public long getCancelledBookings() {
        return cancelledBookings;
    }

    public void setCancelledBookings(long cancelledBookings) {
        this.cancelledBookings = cancelledBookings;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public double getAverageBookingValue() {
        return averageBookingValue;
    }

    public void setAverageBookingValue(double averageBookingValue) {
        this.averageBookingValue = averageBookingValue;
    }
}
