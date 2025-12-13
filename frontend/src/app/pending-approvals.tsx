import { useEffect, useState, useCallback } from 'react'
import { getPendingApprovalBookings, approveBooking, rejectBooking, type Booking } from '@/lib/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'

export default function PendingApprovalsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null)
  const [showApproveModal, setShowApproveModal] = useState(false)
  const [showRejectModal, setShowRejectModal] = useState(false)
  const [deliveryMethod, setDeliveryMethod] = useState<'PICKUP' | 'SHIPPING'>('PICKUP')
  const [pickupLocation, setPickupLocation] = useState('')
  const [rejectionReason, setRejectionReason] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const userData = typeof window !== 'undefined' ? localStorage.getItem('user') : null
  const user = userData ? JSON.parse(userData) : null
  const userId = user?.id

  const fetchPendingBookings = useCallback(async () => {
    if (!userId) {
      window.location.href = '/login'
      return
    }

    try {
      const data = await getPendingApprovalBookings(userId)
      setBookings(data)
    } catch (err: unknown) {
      console.error(err)
      setError('Failed to load pending bookings')
    } finally {
      setLoading(false)
    }
  }, [userId])

  useEffect(() => {
    void fetchPendingBookings()
  }, [fetchPendingBookings])

  const handleApproveClick = (booking: Booking) => {
    setSelectedBooking(booking)
    setDeliveryMethod('PICKUP')
    setPickupLocation('')
    setShowApproveModal(true)
  }

  const handleRejectClick = (booking: Booking) => {
    setSelectedBooking(booking)
    setRejectionReason('')
    setShowRejectModal(true)
  }

  const handleApproveSubmit = async () => {
    if (!selectedBooking || !userId) return

    if (deliveryMethod === 'PICKUP' && !pickupLocation.trim()) {
      alert('Please provide pickup location')
      return
    }

    setSubmitting(true)
    try {
      await approveBooking(userId, selectedBooking.id, {
        deliveryMethod,
        pickupLocation: deliveryMethod === 'PICKUP' ? pickupLocation : undefined
      })
      setShowApproveModal(false)
      await fetchPendingBookings()
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err)
      alert(message || 'Failed to approve booking')
    } finally {
      setSubmitting(false)
    }
  }

  const handleRejectSubmit = async () => {
    if (!selectedBooking || !userId) return

    setSubmitting(true)
    try {
      await rejectBooking(userId, selectedBooking.id, {
        reason: rejectionReason.trim() || undefined
      })
      setShowRejectModal(false)
      await fetchPendingBookings()
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : String(err)
      alert(message || 'Failed to reject booking')
    } finally {
      setSubmitting(false)
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-GB', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
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
              <h1 className="text-2xl font-bold">Pending Booking Approvals</h1>
              <p className="text-sm text-muted-foreground">
                Review and approve or reject booking requests for your products
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
                  No pending booking approvals at the moment.
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
                        Requested by: {booking.renterName}
                      </p>
                    </div>
                    <span className="px-3 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                      Pending Approval
                    </span>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-4">
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
                  </div>
                  <div className="flex gap-2">
                    <Button 
                      onClick={() => handleApproveClick(booking)}
                      className="bg-green-600 hover:bg-green-700"
                    >
                      Approve
                    </Button>
                    <Button 
                      onClick={() => handleRejectClick(booking)}
                      variant="destructive"
                    >
                      Reject
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </main>

      {/* Approve Modal */}
      <Dialog open={showApproveModal} onOpenChange={setShowApproveModal}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Approve Booking</DialogTitle>
            <DialogDescription>
              Choose the delivery method for this booking
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>Delivery Method</Label>
              <div className="flex gap-4">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="deliveryMethod"
                    value="PICKUP"
                    checked={deliveryMethod === 'PICKUP'}
                    onChange={() => setDeliveryMethod('PICKUP')}
                    className="w-4 h-4"
                  />
                  <span>Pick-up at Location</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    name="deliveryMethod"
                    value="SHIPPING"
                    checked={deliveryMethod === 'SHIPPING'}
                    onChange={() => setDeliveryMethod('SHIPPING')}
                    className="w-4 h-4"
                  />
                  <span>Shipping by Mail</span>
                </label>
              </div>
            </div>

            {deliveryMethod === 'PICKUP' && (
              <div className="space-y-2">
                <Label htmlFor="pickupLocation">Pick-up Location / Instructions</Label>
                <Textarea
                  id="pickupLocation"
                  placeholder="Enter the address or instructions for pick-up..."
                  value={pickupLocation}
                  onChange={(e) => setPickupLocation(e.target.value)}
                  rows={3}
                />
              </div>
            )}

            {deliveryMethod === 'SHIPPING' && (
              <p className="text-sm text-muted-foreground">
                A delivery code will be generated automatically after the customer completes payment.
              </p>
            )}
          </div>
          <div className="flex gap-2 justify-end">
            <Button variant="outline" onClick={() => setShowApproveModal(false)}>
              Cancel
            </Button>
            <Button 
              onClick={handleApproveSubmit}
              disabled={submitting}
              className="bg-green-600 hover:bg-green-700"
            >
              {submitting ? 'Approving...' : 'Approve Booking'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Reject Modal */}
      <Dialog open={showRejectModal} onOpenChange={setShowRejectModal}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Reject Booking</DialogTitle>
            <DialogDescription>
              Optionally provide a reason for rejecting this booking
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="rejectionReason">Reason (Optional)</Label>
              <Textarea
                id="rejectionReason"
                placeholder="Enter the reason for rejection..."
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={3}
              />
            </div>
          </div>
          <div className="flex gap-2 justify-end">
            <Button variant="outline" onClick={() => setShowRejectModal(false)}>
              Cancel
            </Button>
            <Button 
              onClick={handleRejectSubmit}
              disabled={submitting}
              variant="destructive"
            >
              {submitting ? 'Rejecting...' : 'Reject Booking'}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
