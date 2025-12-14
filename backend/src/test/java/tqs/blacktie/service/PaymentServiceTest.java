package tqs.blacktie.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.dto.PaymentIntentRequest;
import tqs.blacktie.dto.PaymentIntentResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private StripeClient stripeClient;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(bookingRepository, stripeClient);
    }

    @Test
    void createPaymentIntentShouldBuildStripeRequest() throws StripeException {
        Long userId = 1L;
        Long bookingId = 10L;
        PaymentIntentRequest request = new PaymentIntentRequest(bookingId, 5000L);

        Booking booking = buildBooking(userId, bookingId);

        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getClientSecret()).thenReturn("secret_value");
        when(paymentIntent.getId()).thenReturn("pi_123");
        when(paymentIntent.getAmount()).thenReturn(5000L);
        when(paymentIntent.getCurrency()).thenReturn("eur");

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        when(stripeClient.createPaymentIntent(any(PaymentIntentCreateParams.class)))
            .thenReturn(paymentIntent);

        PaymentIntentResponse response = paymentService.createPaymentIntent(userId, request);

        ArgumentCaptor<PaymentIntentCreateParams> captor = ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
        verify(stripeClient).createPaymentIntent(captor.capture());

        PaymentIntentCreateParams params = captor.getValue();
        assertThat(params.getAmount()).isEqualTo(5000L);
        assertThat(params.getCurrency()).isEqualTo("eur");
        assertThat(params.getMetadata()).containsEntry("bookingId", bookingId.toString());
        assertThat(params.getMetadata()).containsEntry("userId", userId.toString());
        assertThat(params.getMetadata()).containsEntry("productName", "Premium Suit");

        assertThat(response.getClientSecret()).isEqualTo("secret_value");
        assertThat(response.getPaymentIntentId()).isEqualTo("pi_123");
        assertThat(response.getAmount()).isEqualTo(5000L);
        assertThat(response.getCurrency()).isEqualTo("eur");
    }

    @Test
    void createPaymentIntentShouldFailForUnknownBooking() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        PaymentIntentRequest request = new PaymentIntentRequest(999L, 1000L);

        assertThrows(IllegalArgumentException.class,
            () -> paymentService.createPaymentIntent(1L, request)
        );
    }

    @Test
    void createPaymentIntentShouldFailForUnauthorizedUser() {
        Booking booking = buildBooking(2L, 15L);
        when(bookingRepository.findById(15L)).thenReturn(Optional.of(booking));

        PaymentIntentRequest request = new PaymentIntentRequest(15L, 1000L);

        assertThrows(IllegalStateException.class,
            () -> paymentService.createPaymentIntent(1L, request)
        );
    }


    @Test
    void confirmPaymentShouldReturnTrueWhenSucceeded() throws StripeException {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getStatus()).thenReturn("succeeded");
        when(stripeClient.retrievePaymentIntent("pi_succeeded")).thenReturn(paymentIntent);

        boolean confirmed = paymentService.confirmPayment("pi_succeeded");

        assertThat(confirmed).isTrue();
    }

    @Test
    void confirmPaymentShouldReturnFalseOtherwise() throws StripeException {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(paymentIntent.getStatus()).thenReturn("processing");
        when(stripeClient.retrievePaymentIntent("pi_processing")).thenReturn(paymentIntent);

        boolean confirmed = paymentService.confirmPayment("pi_processing");

        assertFalse(confirmed);
    }

    @Test
    void getPaymentIntentShouldDelegateToStripe() throws StripeException {
        PaymentIntent paymentIntent = mock(PaymentIntent.class);
        when(stripeClient.retrievePaymentIntent("pi_get")).thenReturn(paymentIntent);

        PaymentIntent result = paymentService.getPaymentIntent("pi_get");

        assertThat(result).isEqualTo(paymentIntent);
    }

    private Booking buildBooking(Long userId, Long bookingId) {
        User renter = new User("Renter", "renter@example.com", "pass", "renter");
        renter.setId(userId);

        Product product = new Product("Premium Suit", "Formal suit", 200.0);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setRenter(renter);
        booking.setProduct(product);
        return booking;
    }
}
