package tqs.blacktie.controller;

import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tqs.blacktie.dto.PaymentIntentRequest;
import tqs.blacktie.dto.PaymentIntentResponse;
import tqs.blacktie.dto.PaymentStatusResponse;
import tqs.blacktie.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-payment-intent")
    public ResponseEntity<?> createPaymentIntent(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentIntentRequest request) {
        try {
            PaymentIntentResponse response = paymentService.createPaymentIntent(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Payment processing error: " + e.getMessage());
        }
    }

    @GetMapping("/status/{paymentIntentId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentIntentId) {
        try {
            boolean confirmed = paymentService.confirmPayment(paymentIntentId);
            return ResponseEntity.ok(new PaymentStatusResponse(confirmed ? "succeeded" : "pending"));
        } catch (StripeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error checking payment status: " + e.getMessage());
        }
    }
}
