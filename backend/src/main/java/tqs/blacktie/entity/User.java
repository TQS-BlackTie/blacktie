package tqs.blacktie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
public class User {

    // Role constants
    public static final String ROLE_RENTER = "renter";
    public static final String ROLE_OWNER = "owner";
    public static final String ROLE_ADMIN = "admin";

    // Status constants
    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_SUSPENDED = "suspended";
    public static final String STATUS_BANNED = "banned";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = ROLE_RENTER;

    @Column(columnDefinition = "varchar(255) default 'active'")
    private String status = STATUS_ACTIVE;

    @Column
    private String phone;

    @Column
    private String address;

    @Column(length = 500)
    private String businessInfo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User() {
        this.createdAt = LocalDateTime.now();
        this.role = ROLE_RENTER;
        this.status = STATUS_ACTIVE;
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = ROLE_RENTER;
        this.status = STATUS_ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public User(String name, String email, String password, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = STATUS_ACTIVE;
        this.createdAt = LocalDateTime.now();
    }
}
