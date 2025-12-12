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

function CheckoutForm({ booking, onSuccess, onCancel }: Omit<PaymentFormProps, 'userId'>) {
  const stripe = useStripe()
  const elements = useElements()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

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
        <h3 className="font-semibold mb-2">Payment Summary</h3>
        <p className="text-sm text-gray-600">Product: {booking.productName}</p>
        <p className="text-sm text-gray-600">
          Dates: {new Date(booking.bookingDate).toLocaleDateString()} - {new Date(booking.returnDate).toLocaleDateString()}
        </p>
        <p className="text-lg font-bold mt-2">Total: €{booking.totalPrice.toFixed(2)}</p>
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
          {loading ? 'Processing...' : `Pay €${booking.totalPrice.toFixed(2)}`}
        </Button>
      </div>
    </form>
  )
}

export function PaymentModal({ booking, userId, onSuccess, onCancel }: PaymentFormProps) {
  const [clientSecret, setClientSecret] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useState(() => {
    const initPayment = async () => {
      try {
        setLoading(true)
        const amountInCents = Math.round(booking.totalPrice * 100)
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
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full shadow-xl">
        <h2 className="text-2xl font-bold mb-4">Complete Payment</h2>

        {loading && (
          <div className="text-center py-8">
            <p>Loading payment form...</p>
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
            <CheckoutForm booking={booking} onSuccess={onSuccess} onCancel={onCancel} />
          </Elements>
        )}
      </div>
    </div>
  )
}
