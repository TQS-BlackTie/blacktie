import { useEffect, useState } from 'react'
import { getRenterHistory, type Booking, getReviewByBooking } from '@/lib/api'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import ReviewModal from '@/components/ReviewModal'
import ReviewDisplay from '@/components/ReviewDisplay'

export default function BookingHistoryPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [openReviewFor, setOpenReviewFor] = useState<number | null>(null)
  const [openReviewDisplayFor, setOpenReviewDisplayFor] = useState<number | null>(null)
  const [reviewsMap, setReviewsMap] = useState<Record<number, any>>({})

  const userId = (() => {
    const userData = localStorage.getItem('user')
    if (userData) {
      try {
        return JSON.parse(userData).id
      } catch {
        return null
      }
    }
    return null
  })()

  useEffect(() => {
    async function fetchHistory() {
      if (!userId) {
        window.location.href = '/login'
        return
      }

      try {
        const history = await getRenterHistory(userId)
        setBookings(history)
        // fetch existing reviews for completed bookings
        const map: Record<number, any> = {}
        await Promise.all(history.filter(h => h.status === 'COMPLETED').map(async (b) => {
          try {
            const r = await getReviewByBooking(b.id)
            if (r) map[b.id] = r
          } catch {
            // ignore
          }
        }))
        setReviewsMap(map)
      } catch (e) {
        console.error(e)
        setError("Failed to load booking history")
      } finally {
        setLoading(false)
      }
    }

    fetchHistory()
  }, [userId])

  const handleBack = () => {
    window.location.href = '/'
  }

  const handleViewProduct = () => {
    // Navigate to product details - for now just go to home
    window.location.href = '/'
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
      COMPLETED: 'bg-green-100 text-green-800',
      CANCELLED: 'bg-red-100 text-red-800',
      ACTIVE: 'bg-blue-100 text-blue-800'
    }
    return statusColors[status as keyof typeof statusColors] || 'bg-gray-100 text-gray-800'
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <div>Loading...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen p-6 bg-slate-50">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold">Booking History</h1>
            <p className="text-sm text-muted-foreground">
              View your completed and cancelled bookings
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
                No booking history found. Start renting to see your history here!
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
                    {booking.status}
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
                    <p className="text-sm font-medium text-muted-foreground">Total Paid</p>
                    <p className="text-sm font-bold">€{booking.totalPrice.toFixed(2)}</p>
                  </div>
                  <div className="flex items-end">
                    <div className="flex items-center gap-2">
                      <Button onClick={() => handleViewProduct()} variant="outline" size="sm">View Product</Button>
                      {booking.status === 'COMPLETED' && (
                        reviewsMap[booking.id] ? (
                          <Button variant="ghost" size="sm" onClick={() => setOpenReviewDisplayFor(booking.id)}>View Review</Button>
                        ) : (
                          <Button variant="default" size="sm" onClick={() => setOpenReviewFor(booking.id)}>Leave Review</Button>
                        )
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>
      {openReviewFor && userId && (
        <ReviewModal
          bookingId={openReviewFor}
          userId={userId}
          onClose={() => setOpenReviewFor(null)}
          onSuccess={(r) => {
            setReviewsMap(prev => ({ ...prev, [r.bookingId ?? openReviewFor!]: r }))
            if (r.productId) {
              try {
                window.dispatchEvent(new CustomEvent('review:created', { detail: { productId: r.productId } }))
              } catch (e) {
                // ignore
              }
            }
          }}
        />
      )}

      {openReviewDisplayFor && reviewsMap[openReviewDisplayFor] && (
        <ReviewDisplay review={reviewsMap[openReviewDisplayFor]} onClose={() => setOpenReviewDisplayFor(null)} />
      )}
    </div>
  )
}
