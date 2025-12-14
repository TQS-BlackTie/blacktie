package tqs.blacktie.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.service.BookingService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingController Owner History Additional Tests")
class BookingControllerOwnerHistoryTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    @Test
    @DisplayName("Should return empty list when owner has no bookings")
    void shouldReturnEmptyListWhenNoOwnerBookings() {
        when(bookingService.getOwnerBookings(42L)).thenReturn(Collections.emptyList());

        ResponseEntity<List<BookingResponse>> response = bookingController.getOwnerBookings(42L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
        verify(bookingService, times(1)).getOwnerBookings(42L);
    }

    @Test
    @DisplayName("Should propagate exception if service throws when fetching owner bookings")
    void shouldPropagateErrorWhenServiceThrows() {
        when(bookingService.getOwnerBookings(999L)).thenThrow(new IllegalArgumentException("Owner not found"));

        try {
            bookingController.getOwnerBookings(999L);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("Owner not found");
        }

        verify(bookingService, times(1)).getOwnerBookings(999L);
    }
}
