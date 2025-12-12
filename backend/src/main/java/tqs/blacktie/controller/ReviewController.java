package tqs.blacktie.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.service.ReviewService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<?> createReview(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("bookingId") Long bookingId,
            @RequestParam("rating") Integer rating,
            @RequestParam(value = "comment", required = false) String comment
    ) {
        try {
            ReviewResponse resp = reviewService.createReview(userId, bookingId, rating, comment);
            return ResponseEntity.status(HttpStatus.CREATED).body(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ReviewResponse> getByBooking(@PathVariable Long bookingId) {
        ReviewResponse resp = reviewService.getReviewByBooking(bookingId);
        if (resp == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ReviewResponse>> getByProduct(@PathVariable Long productId) {
        List<ReviewResponse> list = reviewService.getReviewsByProduct(productId);
        return ResponseEntity.ok(list);
    }
}
