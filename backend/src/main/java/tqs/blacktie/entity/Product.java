package tqs.blacktie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@Setter
@AllArgsConstructor
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double price;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "available")
    private Boolean available;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    public Product() {
        this.createdAt = LocalDateTime.now();
    }

    public Product(String name, String description, Double price) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.createdAt = LocalDateTime.now();
    }
}
