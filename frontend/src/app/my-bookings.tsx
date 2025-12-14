import { useEffect, useState, useCallback } from 'react'
import { getUserBookings, processBookingPayment, cancelBooking, type Booking } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js'
import { createPaymentIntent } from '@/lib/api'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || '')

type CountdownState = {
  [bookingId: number]: string
}

function calculateCountdown(booking: Booking): { label: string; countdown: string } {
  const now = new Date()
  const bookingDate = new Date(booking.bookingDate)
  const returnDate = new Date(booking.returnDate)

  let targetDate: Date
  let label: string

  if (now < bookingDate) {
    targetDate = bookingDate
    label = "Starts in"
  } else if (now < returnDate) {
    targetDate = returnDate
    label = "Ends in"
  } else {
    return { label: "Status", countdown: "Completed" }
  }

  const diff = targetDate.getTime() - now.getTime()

  if (diff <= 0) {
    return { label, countdown: "Now" }
  }

  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const seconds = Math.floor((diff % (1000 * 60)) / 1000)

  let countdown = ""
  if (days > 0) countdown += `${days}d `
  if (hours > 0 || days > 0) countdown += `${hours}h `
  if (minutes > 0 || hours > 0 || days > 0) countdown += `${minutes}m `
  countdown += `${seconds}s`

  return { label, countdown: countdown.trim() }
}

function PaymentForm({ amount, onSuccess, onCancel }: { 
  amount: number
  onSuccess: () => void
  onCancel: () => void
}) {
  const stripe = useStripe()
  // bookingId is used for payment processing context
  const elements = useElements()
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!stripe || !elements) return

    setProcessing(true)
    setError('')

    try {
      const { error: submitError } = await stripe.confirmPayment({
        elements,
        redirect: 'if_required',
      })

      if (submitError) {
        setError(submitError.message || 'Payment failed')
      } else {
        onSuccess()
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err)
      setError(message || 'Payment failed')
    } finally {
      setProcessing(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <PaymentElement />
      {error && (
        <div className="text-red-600 text-sm bg-red-50 p-3 rounded">
          {error}
        </div>
      )}
      <div className="flex gap-2">
        <Button type="button" variant="outline" onClick={onCancel} disabled={processing}>
          Cancel
        </Button>
        <Button type="submit" disabled={!stripe || processing} className="flex-1">
          {processing ? 'Processing...' : `Pay €${(amount / 100).toFixed(2)}`}
        </Button>
      </div>
    </form>
  )
}

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null)
  const [showPaymentModal, setShowPaymentModal] = useState(false)
  const [clientSecret, setClientSecret] = useState<string | null>(null)
  const [processingPayment, setProcessingPayment] = useState(false)
  const [countdowns, setCountdowns] = useState<CountdownState>({})
  const [cancellingId, setCancellingId] = useState<number | null>(null)

  const userData = typeof window !== 'undefined' ? localStorage.getItem('user') : null
  const user = userData ? JSON.parse(userData) : null
  const userId = user?.id

  const fetchBookings = useCallback(async () => {
    if (!userId) {
      window.location.href = '/login'
      return
    }

    try {
      const data = await getUserBookings(userId)
      // Filter to show only active bookings (not COMPLETED or CANCELLED)
      setBookings(data.filter(b => b.status !== 'COMPLETED' && b.status !== 'CANCELLED'))
    } catch (err: unknown) {
      console.error(err)
      setError('Failed to load bookings')
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    void fetchBookings()
  }, [fetchBookings])

  // Update countdowns every second for PAID and COMPLETED bookings
  useEffect(() => {
    if (bookings.length === 0) return

    const updateCountdowns = () => {
      const newCountdowns: CountdownState = {}
      bookings
        .filter(b => b.status === 'PAID' || b.status === 'COMPLETED')
        .forEach((booking) => {
          const { countdown } = calculateCountdown(booking)
          newCountdowns[booking.id] = countdown
        })
      setCountdowns(newCountdowns)
    }

    updateCountdowns()
    const interval = setInterval(updateCountdowns, 1000)
    return () => clearInterval(interval)
  }, [bookings])

  

  const handlePayClick = async (booking: Booking) => {
    setSelectedBooking(booking)
    setProcessingPayment(true)
    
    try {
      const paymentIntent = await createPaymentIntent(userId, {
        bookingId: booking.id,
        amount: Math.round(booking.totalPrice * 100)
      })
      setClientSecret(paymentIntent.clientSecret)
      setShowPaymentModal(true)
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err)
      alert(message || 'Failed to initiate payment')
    } finally {
      setProcessingPayment(false)
    }
  }

  const handlePaymentSuccess = async () => {
    if (!selectedBooking || !userId) return

    try {
      await processBookingPayment(userId, selectedBooking.id)
      setShowPaymentModal(false)
      setClientSecret(null)
      await fetchBookings()
      alert('Payment successful! Check your booking details for delivery information.')
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err)
      alert(message || 'Failed to process payment')
    }
  }

  const handleCancel = async (bookingId: number) => {
    if (!confirm("Are you sure you want to cancel this booking?")) return
    if (!userId) return

    try {
      setCancellingId(bookingId)
      await cancelBooking(userId, bookingId)
      await fetchBookings()
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err)
      alert(message || "Failed to cancel booking")
    } finally {
      setCancellingId(null)
    }
  }

  const canCancel = (booking: Booking): boolean => {
    const now = new Date()
    const bookingDate = new Date(booking.bookingDate)
    return now < bookingDate && ['PENDING_APPROVAL', 'APPROVED'].includes(booking.status)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-GB', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    })
  }

  const getStatusBadge = (status: string) => {
    const statusColors = {
      PENDING_APPROVAL: 'bg-yellow-100 text-yellow-800',
      APPROVED: 'bg-blue-100 text-blue-800',
      REJECTED: 'bg-red-100 text-red-800',
      PAID: 'bg-green-100 text-green-800',
      COMPLETED: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800'
    }
    return statusColors[status as keyof typeof statusColors] || 'bg-gray-100 text-gray-800'
  }

  const getStatusLabel = (status: string) => {
    const labels: Record<string, string> = {
      PENDING_APPROVAL: 'Pending Approval',
      APPROVED: 'Approved - Ready to Pay',
      REJECTED: 'Rejected',
      PAID: 'Paid',
      COMPLETED: 'Completed',
      CANCELLED: 'Cancelled'
    }
    return labels[status] || status
  }

  const handleLogout = () => {
    localStorage.removeItem('user')
    window.location.href = '/login'
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div>Loading...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800 text-white">
      <Navbar
        userName={user?.name}
        userRole={user?.role}
        onLogout={handleLogout}
        notificationBell={<NotificationBell userId={userId} />}
      />

      <main className="relative z-10 w-full px-6 pb-12 mt-8 md:px-12 lg:px-20">
        <div className="max-w-7xl mx-auto">
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-white">My Bookings</h1>
            <p className="text-slate-200 mt-2">
              View and manage your rentals
            </p>
          </div>

          {error && (
            <div className="text-red-600 bg-red-50 p-4 rounded-lg mb-4 border border-red-200">
              {error}
            </div>
          )}

          {bookings.length === 0 && !error && (
            <div className="rounded-3xl border border-white/15 bg-white/75 p-8 text-center shadow-2xl backdrop-blur">
              <p className="text-slate-600">
                No bookings yet. Start renting to see your bookings here!
              </p>
            </div>
          )}

          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            {bookings.map((booking) => {
              const { label } = calculateCountdown(booking)
              const countdown = countdowns[booking.id] || ""
              const showCountdown = booking.status === 'PAID' || booking.status === 'COMPLETED'

              return (
                <div
                  key={booking.id}
                  className="group rounded-3xl border border-white/15 bg-white/75 p-6 shadow-2xl backdrop-blur transition-all duration-300 hover:shadow-emerald-500/10 hover:-translate-y-1"
                >
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <h3 className="text-lg font-bold text-slate-900">{booking.productName}</h3>
                      <p className="text-sm text-slate-600">
                        Owner: {booking.ownerName || 'Unknown'}
                      </p>
                    </div>
                    <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusBadge(booking.status)}`}>
                      {getStatusLabel(booking.status)}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                      <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">From</p>
                      <p className="text-sm font-semibold text-slate-900">{formatDate(booking.bookingDate)}</p>
                    </div>
                    <div>
                      <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">To</p>
                      <p className="text-sm font-semibold text-slate-900">{formatDate(booking.returnDate)}</p>
                    </div>
                  </div>

                  {showCountdown && countdown && (
                    <div className="rounded-2xl bg-gradient-to-br from-emerald-50 via-cyan-50 to-blue-50 p-4 mb-4 border border-emerald-100">
                      <p className="text-xs font-medium text-emerald-700 uppercase tracking-wider">{label}</p>
                      <p className="text-2xl font-bold bg-gradient-to-r from-emerald-600 via-cyan-600 to-blue-600 bg-clip-text text-transparent">
                        {countdown}
                      </p>
                    </div>
                  )}

                  {booking.status === 'PENDING_APPROVAL' && (
                    <div className="rounded-2xl bg-yellow-50 p-4 mb-4 border border-yellow-100">
                      <p className="text-sm text-yellow-700">
                        ⏳ Waiting for owner approval...
                      </p>
                    </div>
                  )}

                  {booking.status === 'REJECTED' && booking.rejectionReason && (
                    <div className="rounded-2xl bg-red-50 p-4 mb-4 border border-red-100">
                      <p className="text-xs font-medium text-red-600 uppercase tracking-wider mb-1">Rejection Reason</p>
                      <p className="text-sm text-red-800">{booking.rejectionReason}</p>
                    </div>
                  )}

                  {booking.status === 'APPROVED' && (
                    <div className="rounded-2xl bg-blue-50 p-4 mb-4 border border-blue-100">
                      <p className="text-sm text-blue-700 mb-3">
                        ✅ Booking approved! Proceed with payment.
                      </p>
                      <Button 
                        onClick={() => handlePayClick(booking)}
                        disabled={processingPayment}
                        className="w-full bg-green-600 hover:bg-green-700 rounded-full"
                        size="sm"
                      >
                        {processingPayment ? 'Loading...' : 'Pay Now'}
                      </Button>
                    </div>
                  )}

                  {booking.status === 'PAID' && booking.deliveryMethod && (
                    <div className="rounded-2xl bg-green-50 p-4 mb-4 border border-green-100">
                      <p className="text-xs font-medium text-green-800 uppercase tracking-wider mb-2">✅ Payment Confirmed</p>
                      <p className="text-sm font-medium">
                        Delivery: {booking.deliveryMethod === 'PICKUP' ? 'Pick-up' : 'Shipping'}
                      </p>
                      {booking.deliveryMethod === 'SHIPPING' && booking.deliveryCode && (
                        <>
                          <p className="text-xs font-medium text-green-700 mt-2 uppercase tracking-wider">Delivery Code</p>
                          <p className="text-lg font-mono font-bold text-green-700">{booking.deliveryCode}</p>
                        </>
                      )}
                      {booking.deliveryMethod === 'PICKUP' && booking.pickupLocation && (
                        <>
                          <p className="text-xs font-medium text-green-700 mt-2 uppercase tracking-wider">Pick-up Location</p>
                          <p className="text-sm text-green-800">{booking.pickupLocation}</p>
                        </>
                      )}
                    </div>
                  )}

                  <div className="flex items-center justify-between">
                    <p className="text-lg font-bold text-slate-900">€{booking.totalPrice.toFixed(2)}</p>
                    {canCancel(booking) && (
                      <Button
                        onClick={() => handleCancel(booking.id)}
                        disabled={cancellingId === booking.id}
                        variant="destructive"
                        size="sm"
                        className="rounded-full"
                      >
                        {cancellingId === booking.id ? "Cancelling..." : "Cancel"}
                      </Button>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        </div>
      </main>

      {showPaymentModal && clientSecret && (
        <Dialog open={showPaymentModal} onOpenChange={() => {
          setShowPaymentModal(false)
          setClientSecret(null)
        }}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Complete Payment</DialogTitle>
            </DialogHeader>
            <Elements stripe={stripePromise} options={{ clientSecret }}>
              <PaymentForm
                amount={Math.round(selectedBooking!.totalPrice * 100)}
                onSuccess={handlePaymentSuccess}
                onCancel={() => {
                  setShowPaymentModal(false)
                  setClientSecret(null)
                }}
              />
            </Elements>
          </DialogContent>
        </Dialog>
      )}
    </div>
  )
}
