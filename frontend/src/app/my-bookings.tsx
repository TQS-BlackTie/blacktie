import { useEffect, useState } from 'react'
import { getUserBookings, processBookingPayment, type Booking } from '@/lib/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'
import { loadStripe } from '@stripe/stripe-js'
import { Elements, PaymentElement, useStripe, useElements } from '@stripe/react-stripe-js'
import { createPaymentIntent } from '@/lib/api'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'

const stripePromise = loadStripe(import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY || '')

function PaymentForm({ bookingId: _bookingId, amount, onSuccess, onCancel }: { 
  bookingId: number
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
    } catch (err: any) {
      setError(err.message || 'Payment failed')
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

  const userData = typeof window !== 'undefined' ? localStorage.getItem('user') : null
  const user = userData ? JSON.parse(userData) : null
  const userId = user?.id

  useEffect(() => {
    fetchBookings()
  }, [userId])

  const fetchBookings = async () => {
    if (!userId) {
      window.location.href = '/login'
      return
    }

    try {
      const data = await getUserBookings(userId)
      setBookings(data)
    } catch (e) {
      console.error(e)
      setError("Failed to load bookings")
    } finally {
      setLoading(false)
    }
  }

  const handlePayClick = async (booking: Booking) => {
    setSelectedBooking(booking)
    setProcessingPayment(true)
    
    try {
      const paymentIntent = await createPaymentIntent(userId, {
        bookingId: booking.id,
        amount: Math.round(booking.totalPrice * 100) // Convert to cents
      })
      setClientSecret(paymentIntent.clientSecret)
      setShowPaymentModal(true)
    } catch (e: any) {
      alert(e.message || 'Failed to initiate payment')
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
    } catch (e: any) {
      alert(e.message || 'Failed to process payment')
    }
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

  const handleBack = () => {
    window.location.href = '/'
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
    <div className="relative min-h-screen overflow-hidden text-slate-50">
      <div className="aurora-blob -left-10 top-8 h-64 w-64 bg-emerald-500/40" />
      <div className="aurora-blob delay-1 right-0 top-40 h-64 w-64 bg-cyan-400/30" />
      <div className="aurora-blob delay-2 -bottom-10 left-20 h-64 w-64 bg-blue-500/25" />

      <Navbar
        userName={user?.name}
        userRole={user?.role}
        onLogout={handleLogout}
        notificationBell={<NotificationBell userId={userId} />}
      />

      <main className="relative z-10 w-full px-6 pb-12 mt-20 md:px-12 lg:px-20">
        <div className="rounded-3xl border border-white/15 bg-white/75 p-4 text-slate-900 shadow-2xl backdrop-blur md:p-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-2xl font-bold">My Active Bookings</h1>
              <p className="text-sm text-muted-foreground">
                View and manage your current bookings
              </p>
            </div>
            <Button onClick={handleBack} variant="outline">
              Back to Home
            </Button>
          </div>

          {error && (
            <div className="text-red-600 bg-red-50 p-4 rounded-lg mb-4">
              {error}
            </div>
          )}

          {bookings.length === 0 && !error && (
            <Card>
              <CardContent className="pt-6">
                <p className="text-center text-muted-foreground">
                  No active bookings. Start renting to see your bookings here!
                </p>
              </CardContent>
            </Card>
          )}

          <div className="space-y-4">
            {bookings.map((booking) => (
              <Card key={booking.id} className="hover:shadow-md transition-shadow">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle className="text-lg">{booking.productName}</CardTitle>
                      <p className="text-sm text-muted-foreground">
                        Owner: {booking.ownerName || 'Unknown'}
                      </p>
                    </div>
                    <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusBadge(booking.status)}`}>
                      {getStatusLabel(booking.status)}
                    </span>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm font-medium text-muted-foreground">Booking Date</p>
                      <p className="text-sm">{formatDate(booking.bookingDate)}</p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-muted-foreground">Return Date</p>
                      <p className="text-sm">{formatDate(booking.returnDate)}</p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-muted-foreground">Total Price</p>
                      <p className="text-sm font-bold">€{booking.totalPrice.toFixed(2)}</p>
                    </div>

                    {booking.status === 'PENDING_APPROVAL' && (
                      <div className="md:col-span-2">
                        <p className="text-sm text-yellow-700">
                          ⏳ Waiting for owner approval...
                        </p>
                      </div>
                    )}

                    {booking.status === 'REJECTED' && booking.rejectionReason && (
                      <div className="md:col-span-2">
                        <p className="text-sm font-medium text-red-600">Rejection Reason</p>
                        <p className="text-sm text-red-800">{booking.rejectionReason}</p>
                      </div>
                    )}

                    {booking.status === 'APPROVED' && (
                      <div className="md:col-span-2">
                        <p className="text-sm text-blue-700 mb-3">
                          ✅ Your booking has been approved! Proceed with payment to confirm.
                        </p>
                        <Button 
                          onClick={() => handlePayClick(booking)}
                          disabled={processingPayment}
                          className="bg-green-600 hover:bg-green-700"
                        >
                          {processingPayment ? 'Loading...' : 'Pay Now'}
                        </Button>
                      </div>
                    )}

                    {booking.status === 'PAID' && booking.deliveryMethod && (
                      <div className="md:col-span-2 bg-green-50 p-3 rounded-lg">
                        <p className="text-sm font-medium text-green-800 mb-2">✅ Payment Confirmed!</p>
                        <p className="text-sm font-medium">Delivery Method: {booking.deliveryMethod === 'PICKUP' ? 'Pick-up' : 'Shipping'}</p>
                        {booking.deliveryMethod === 'SHIPPING' && booking.deliveryCode && (
                          <>
                            <p className="text-sm font-medium mt-2">Delivery Code:</p>
                            <p className="text-lg font-mono font-bold text-green-700">{booking.deliveryCode}</p>
                          </>
                        )}
                        {booking.deliveryMethod === 'PICKUP' && booking.pickupLocation && (
                          <>
                            <p className="text-sm font-medium mt-2">Pick-up Location:</p>
                            <p className="text-sm">{booking.pickupLocation}</p>
                          </>
                        )}
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </main>

      {/* Payment Modal */}
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
                bookingId={selectedBooking!.id}
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
