package tqs.blacktie.controller;

import com.stripe.exception.StripeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tqs.blacktie.dto.PaymentIntentRequest;
import tqs.blacktie.dto.PaymentIntentResponse;
import tqs.blacktie.dto.PaymentStatusResponse;
import tqs.blacktie.service.PaymentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void shouldCreatePaymentIntentSuccessfully() throws StripeException {
        PaymentIntentResponse response = new PaymentIntentResponse("secret", "pi_123", 5000L, "eur");
        when(paymentService.createPaymentIntent(eq(1L), any(PaymentIntentRequest.class))).thenReturn(response);

        ResponseEntity<?> result = paymentController.createPaymentIntent(1L, new PaymentIntentRequest(10L, 5000L));

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }

    @Test
    void shouldReturnBadRequestForIllegalArgument() throws StripeException {
        when(paymentService.createPaymentIntent(eq(1L), any(PaymentIntentRequest.class)))
            .thenThrow(new IllegalArgumentException("Booking missing"));

        ResponseEntity<?> result = paymentController.createPaymentIntent(1L, new PaymentIntentRequest(10L, 5000L));

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Booking missing", result.getBody());
    }

    @Test
    void shouldReturnForbiddenForIllegalState() throws StripeException {
        when(paymentService.createPaymentIntent(eq(1L), any(PaymentIntentRequest.class)))
            .thenThrow(new IllegalStateException("Unauthorized"));

        ResponseEntity<?> result = paymentController.createPaymentIntent(1L, new PaymentIntentRequest(10L, 5000L));

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        assertEquals("Unauthorized", result.getBody());
    }

    @Test
    void shouldReturnServerErrorForStripeIssues() throws StripeException {
        StripeException stripeError = org.mockito.Mockito.mock(StripeException.class);
        when(stripeError.getMessage()).thenReturn("Stripe down");
        when(paymentService.createPaymentIntent(eq(1L), any(PaymentIntentRequest.class)))
            .thenThrow(stripeError);

        ResponseEntity<?> result = paymentController.createPaymentIntent(1L, new PaymentIntentRequest(10L, 5000L));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Payment processing error: Stripe down", result.getBody());
    }

    @Test
    void shouldReturnStatusFromService() throws StripeException {
        when(paymentService.confirmPayment("pi_status")).thenReturn(true);

        ResponseEntity<?> result = paymentController.getPaymentStatus("pi_status");

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertInstanceOf(PaymentStatusResponse.class, result.getBody());
        PaymentStatusResponse body = (PaymentStatusResponse) result.getBody();
        assertEquals("succeeded", body.status());
    }

    @Test
    void shouldReturnErrorWhenStatusFails() throws StripeException {
        StripeException stripeError = org.mockito.Mockito.mock(StripeException.class);
        when(stripeError.getMessage()).thenReturn("Network issue");
        when(paymentService.confirmPayment("pi_status")).thenThrow(stripeError);

        ResponseEntity<?> result = paymentController.getPaymentStatus("pi_status");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("Error checking payment status: Network issue", result.getBody());
    }
}
