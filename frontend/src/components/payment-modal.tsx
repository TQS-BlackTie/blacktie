import { useState } from 'react'
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js'
import { getStripe } from '@/lib/stripe'
import { Button } from '@/components/ui/button'
import { createPaymentIntent, type Booking } from '@/lib/api'

type PaymentFormProps = {
  booking: Booking
  userId: number
  onSuccess: () => void
  onCancel: () => void
}

function CheckoutForm({ booking, totalAmount, onSuccess, onCancel }: Omit<PaymentFormProps, 'userId'> & { totalAmount: number }) {
  const stripe = useStripe()
  const elements = useElements()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const hasDeposit = booking.productDepositAmount && booking.productDepositAmount > 0
  const depositAmount = hasDeposit ? booking.productDepositAmount! : 0

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!stripe || !elements) {
      return
    }

    setLoading(true)
    setError(null)

    try {
      const { error: submitError } = await stripe.confirmPayment({
        elements,
        confirmParams: {
          return_url: `${window.location.origin}/bookings`,
        },
      })

      if (submitError) {
        setError(submitError.message || 'Payment failed')
      } else {
        onSuccess()
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Payment failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="bg-gray-50 p-4 rounded-lg">
        <h3 className="font-semibold mb-2 text-gray-900">Payment Summary</h3>
        <p className="text-sm text-gray-600">Product: {booking.productName}</p>
        <p className="text-sm text-gray-600">
          Dates: {new Date(booking.bookingDate).toLocaleDateString()} - {new Date(booking.returnDate).toLocaleDateString()}
        </p>

        {/* Price breakdown */}
        <div className="mt-3 pt-3 border-t border-gray-200 space-y-1">
          <div className="flex justify-between text-sm text-gray-600">
            <span>Rental</span>
            <span>€{booking.totalPrice.toFixed(2)}</span>
          </div>
          {hasDeposit && (
            <div className="flex justify-between text-sm text-amber-700">
              <span>Security Deposit (refundable)</span>
              <span>+€{depositAmount.toFixed(2)}</span>
            </div>
          )}
          <div className="flex justify-between font-bold text-gray-900 pt-2 border-t mt-2">
            <span>Total</span>
            <span>€{totalAmount.toFixed(2)}</span>
          </div>
        </div>

        {hasDeposit && (
          <p className="text-xs text-gray-500 mt-2">
            The security deposit will be refunded after the item is returned in good condition.
          </p>
        )}
      </div>

      {error && (
        <div className="text-red-600 text-sm bg-red-50 p-3 rounded">
          {error}
        </div>
      )}

      <PaymentElement />

      <div className="flex gap-2 pt-2">
        <Button
          type="button"
          onClick={onCancel}
          disabled={loading}
          className="flex-1 bg-gray-200 text-gray-800 hover:bg-gray-300"
        >
          Cancel
        </Button>
        <Button
          type="submit"
          disabled={!stripe || loading}
          className="flex-1"
        >
          {loading ? 'Processing...' : `Pay €${totalAmount.toFixed(2)}`}
        </Button>
      </div>
    </form>
  )
}

export function PaymentModal({ booking, userId, onSuccess, onCancel }: PaymentFormProps) {
  const [clientSecret, setClientSecret] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  // Calculate total including deposit
  const hasDeposit = booking.productDepositAmount && booking.productDepositAmount > 0
  const depositAmount = hasDeposit ? booking.productDepositAmount! : 0
  const totalAmount = booking.totalPrice + depositAmount

  useState(() => {
    const initPayment = async () => {
      try {
        setLoading(true)
        // Calculate total in cents (rental + deposit)
        const amountInCents = Math.round(totalAmount * 100)
        const response = await createPaymentIntent(userId, {
          bookingId: booking.id,
          amount: amountInCents,
        })
        setClientSecret(response.clientSecret)
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to initialize payment')
      } finally {
        setLoading(false)
      }
    }
    void initPayment()
  })

  const stripePromise = getStripe()

  if (!stripePromise) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
        <div className="bg-white rounded-lg p-6 max-w-md w-full">
          <p className="text-red-600">Stripe is not configured properly</p>
          <Button onClick={onCancel} className="mt-4 w-full">Close</Button>
        </div>
      </div>
    )
  }

  return (
    <div
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50"
      onClick={(e) => {
        if (e.target === e.currentTarget && !loading) onCancel()
      }}
    >
      <div className="bg-white rounded-lg p-6 max-w-md w-full shadow-xl" style={{ margin: 'auto' }}>
        <h2 className="text-2xl font-bold mb-4 text-gray-900">Complete Payment</h2>

        {loading && (
          <div className="text-center py-8">
            <p className="text-gray-600">Loading payment form...</p>
          </div>
        )}

        {error && (
          <div className="text-red-600 text-sm bg-red-50 p-3 rounded mb-4">
            {error}
            <Button onClick={onCancel} className="mt-4 w-full">Close</Button>
          </div>
        )}

        {clientSecret && !loading && (
          <Elements stripe={stripePromise} options={{ clientSecret }}>
            <CheckoutForm
              booking={booking}
              totalAmount={totalAmount}
              onSuccess={onSuccess}
              onCancel={onCancel}
            />
          </Elements>
        )}
      </div>
    </div>
  )
}
