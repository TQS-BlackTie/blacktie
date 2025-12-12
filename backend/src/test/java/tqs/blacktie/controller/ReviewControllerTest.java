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
    void getByBooking_notFound_returns404() throws Exception {
        when(reviewService.getReviewByBooking(99L)).thenReturn(null);

        mockMvc.perform(get("/api/reviews/booking/99"))
                .andExpect(status().isNotFound());
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
