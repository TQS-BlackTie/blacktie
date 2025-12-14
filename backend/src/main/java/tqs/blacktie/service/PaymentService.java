package tqs.blacktie.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Service;
import tqs.blacktie.dto.PaymentIntentRequest;
import tqs.blacktie.dto.PaymentIntentResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.repository.BookingRepository;

@Service
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final StripeClient stripeClient;

    public PaymentService(BookingRepository bookingRepository, StripeClient stripeClient) {
        this.bookingRepository = bookingRepository;
        this.stripeClient = stripeClient;
    }

    public PaymentIntentResponse createPaymentIntent(Long userId, PaymentIntentRequest request) throws StripeException {
        // Validate booking exists and belongs to user
        Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + request.getBookingId()));

        if (!booking.getRenter().getId().equals(userId)) {
            throw new IllegalStateException("User is not authorized to pay for this booking");
        }

        // Create Stripe PaymentIntent
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
            .setAmount(request.getAmount())
            .setCurrency("eur")
            .putMetadata("bookingId", booking.getId().toString())
            .putMetadata("userId", userId.toString())
            .putMetadata("productName", booking.getProduct().getName())
            .setAutomaticPaymentMethods(
                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                    .setEnabled(true)
                    .build()
            )
            .build();

        PaymentIntent paymentIntent = stripeClient.createPaymentIntent(params);

        return new PaymentIntentResponse(
            paymentIntent.getClientSecret(),
            paymentIntent.getId(),
            paymentIntent.getAmount(),
            paymentIntent.getCurrency()
        );
    }

    public PaymentIntent getPaymentIntent(String paymentIntentId) throws StripeException {
        return stripeClient.retrievePaymentIntent(paymentIntentId);
    }

    public boolean confirmPayment(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = getPaymentIntent(paymentIntentId);
        return "succeeded".equalsIgnoreCase(paymentIntent.getStatus());
    }
}
