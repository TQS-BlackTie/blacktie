import { useState } from "react"
import { createBooking, type Product } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

type BookingModalProps = {
  product: Product
  userId: number
  onClose: () => void
  onSuccess: () => void
}

export function BookingModal({ product, userId, onClose, onSuccess }: BookingModalProps) {
  const [bookingDate, setBookingDate] = useState("")
  const [returnDate, setReturnDate] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const calculatePrice = () => {
    if (!bookingDate || !returnDate) return null
    const start = new Date(bookingDate)
    const end = new Date(returnDate)
    const days = Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
    return days > 0 ? days * product.price : null
  }

  const totalPrice = calculatePrice()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!bookingDate || !returnDate) {
      setError("Please select both dates")
      return
    }

    const start = new Date(bookingDate)
    const end = new Date(returnDate)

    if (end <= start) {
      setError("Return date must be after booking date")
      return
    }

    if (start < new Date()) {
      setError("Booking date cannot be in the past")
      return
    }

    try {
      setLoading(true)
      await createBooking(userId, {
        productId: product.id,
        bookingDate: new Date(bookingDate).toISOString(),
        returnDate: new Date(returnDate).toISOString(),
      })
      onSuccess()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create booking")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full shadow-xl">
        <h2 className="text-2xl font-bold mb-4">Reserve {product.name}</h2>
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <p className="text-sm text-gray-600 mb-2">{product.description}</p>
            <p className="text-lg font-semibold text-gray-900">
              {product.price.toFixed(2)} € / day
            </p>
          </div>

          {error && (
            <div className="text-red-600 text-sm bg-red-50 p-3 rounded">
              {error}
            </div>
          )}

          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium" htmlFor="bookingDate">
              Booking Date
            </label>
            <Input
              id="bookingDate"
              type="datetime-local"
              value={bookingDate}
              onChange={(e) => setBookingDate(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-sm font-medium" htmlFor="returnDate">
              Return Date
            </label>
            <Input
              id="returnDate"
              type="datetime-local"
              value={returnDate}
              onChange={(e) => setReturnDate(e.target.value)}
              required
            />
          </div>

          {totalPrice !== null && (
            <div className="bg-gray-50 p-3 rounded">
              <p className="text-sm text-gray-600">Total Price</p>
              <p className="text-2xl font-bold text-gray-900">
                {totalPrice.toFixed(2)} €
              </p>
            </div>
          )}

          <div className="flex gap-2 pt-2">
            <Button
              type="button"
              onClick={onClose}
              disabled={loading}
              className="flex-1 bg-gray-200 text-gray-800 hover:bg-gray-300"
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={loading || totalPrice === null}
              className="flex-1"
            >
              {loading ? "Reserving..." : "Confirm Reservation"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}
