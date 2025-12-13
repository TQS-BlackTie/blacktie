package tqs.blacktie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "reviews", uniqueConstraints = {@UniqueConstraint(columnNames = {"booking_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private Integer rating;

    @Lob
    private String comment;
    private LocalDateTime createdAt;

    public Review(Booking booking, Integer rating, String comment) {
        this.booking = booking;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }
}
