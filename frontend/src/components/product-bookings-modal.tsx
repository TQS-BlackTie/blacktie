import { useEffect, useState } from "react"
import { cancelBooking, getBookingsByProduct, type Booking, type Product } from "@/lib/api"
import { Button } from "@/components/ui/button"

type ProductBookingsModalProps = {
  product: Product
  userId: number
  onClose: () => void
}

export function ProductBookingsModal({ product, userId, onClose }: ProductBookingsModalProps) {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [canceling, setCanceling] = useState<number | null>(null)

  const loadBookings = async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await getBookingsByProduct(product.id, userId)
      setBookings(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load bookings")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadBookings()
  }, [])

  const handleCancel = async (bookingId: number) => {
    try {
      setCanceling(bookingId)
      setError(null)
      await cancelBooking(userId, bookingId)
      await loadBookings()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to cancel booking")
    } finally {
      setCanceling(null)
    }
  }

  const formatDate = (value: string) => {
    const d = new Date(value)
    return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" })
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg p-6 max-w-2xl w-full shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold">Bookings for {product.name}</h2>
            <p className="text-sm text-gray-600">Manage reservations on this product.</p>
          </div>
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </div>

        {error && (
          <div className="text-red-600 text-sm bg-red-50 p-3 rounded mb-3">
            {error}
          </div>
        )}

        {loading ? (
          <p className="text-sm text-gray-600">Loading bookings...</p>
        ) : bookings.length === 0 ? (
          <p className="text-sm text-gray-600">No bookings for this product yet.</p>
        ) : (
          <div className="divide-y">
            {bookings.map((b) => (
              <div key={b.id} className="py-3 flex items-center justify-between gap-4">
                <div className="space-y-1">
                  <p className="text-sm font-semibold text-gray-900">{b.renterName}</p>
                  <p className="text-sm text-gray-600">
                    {formatDate(b.bookingDate)} → {formatDate(b.returnDate)}
                  </p>
                  <p className="text-xs text-gray-500">Total: {b.totalPrice.toFixed(2)} €</p>
                </div>
                <Button
                  variant="destructive"
                  onClick={() => handleCancel(b.id)}
                  disabled={canceling === b.id}
                >
                  {canceling === b.id ? "Cancelling..." : "Cancel booking"}
                </Button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
