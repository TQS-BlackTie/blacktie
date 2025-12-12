package tqs.blacktie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.service.ReviewService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReviewControllerTest {

    private ReviewService reviewService;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        reviewService = mock(ReviewService.class);
        ReviewController controller = new ReviewController(reviewService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void postReview_success_returnsCreated() throws Exception {
        ReviewResponse resp = new ReviewResponse(1L, 2L, 3L, 5, "ok", LocalDateTime.now());
        when(reviewService.createReview(10L, 2L, 5, "ok")).thenReturn(resp);

        mockMvc.perform(post("/api/reviews")
                .header("X-User-Id", "10")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("bookingId", "2")
                .param("rating", "5")
                .param("comment", "ok")
        )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void postReview_badRequest_mapsTo400() throws Exception {
        when(reviewService.createReview(anyLong(), anyLong(), anyInt(), any())).thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(post("/api/reviews")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("bookingId", "1")
                .param("rating", "5")
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void postReview_forbidden_mapsTo403() throws Exception {
        when(reviewService.createReview(anyLong(), anyLong(), anyInt(), any())).thenThrow(new IllegalStateException("forbidden"));

        mockMvc.perform(post("/api/reviews")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("bookingId", "1")
                .param("rating", "5")
        )
                .andExpect(status().isForbidden());
    }

    @Test
    void getByBooking_notFound_returns404() throws Exception {
        when(reviewService.getReviewByBooking(99L)).thenReturn(null);

        mockMvc.perform(get("/api/reviews/booking/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByBooking_returnsReview() throws Exception {
        ReviewResponse resp = new ReviewResponse(5L, 42L, 10L, 4, "ok", LocalDateTime.now());
        when(reviewService.getReviewByBooking(42L)).thenReturn(resp);

        mockMvc.perform(get("/api/reviews/booking/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getByProduct_returnsList() throws Exception {
        ReviewResponse r1 = new ReviewResponse(5L, 10L, 7L, 4, "nice", LocalDateTime.now());
        when(reviewService.getReviewsByProduct(7L)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/reviews/product/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(5));
    }
}
