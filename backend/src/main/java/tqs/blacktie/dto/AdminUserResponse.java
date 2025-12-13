package tqs.blacktie.dto;

public class AdminUserResponse {
    
    private Long id;
    private String name;
    private String email;
    private String role;
    private String status;
    private String phone;
    private String address;
    private String businessInfo;
    private String createdAt;
    private long bookingsCount;
    private long productsCount;

    public AdminUserResponse() {
    }

    public AdminUserResponse(Long id, String name, String email, String role, String status,
                             String phone, String address, String businessInfo,
                             String createdAt, long bookingsCount, long productsCount) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
        this.phone = phone;
        this.address = address;
        this.businessInfo = businessInfo;
        this.createdAt = createdAt;
        this.bookingsCount = bookingsCount;
        this.productsCount = productsCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public long getBookingsCount() {
        return bookingsCount;
    }

    public void setBookingsCount(long bookingsCount) {
        this.bookingsCount = bookingsCount;
    }

    public long getProductsCount() {
        return productsCount;
    }

    public void setProductsCount(long productsCount) {
        this.productsCount = productsCount;
    }
}
