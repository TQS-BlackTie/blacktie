import { useCallback, useEffect, useState } from "react"
import { cancelBooking, getBookingsByProduct, getReviewsByProduct, deleteProduct, refundDeposit, createReview, type Booking, type Product, type ReviewResponse } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"

type ProductBookingsModalProps = {
  product: Product
  userId: number
  onClose: () => void
  onProductDeleted?: () => void
}

export function ProductBookingsModal({ product, userId, onClose, onProductDeleted }: ProductBookingsModalProps) {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [canceling, setCanceling] = useState<number | null>(null)
  const [reviews, setReviews] = useState<ReviewResponse[]>([])
  const [loadingReviews, setLoadingReviews] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [refunding, setRefunding] = useState<number | null>(null)

  // Review state
  const [reviewingBooking, setReviewingBooking] = useState<number | null>(null)
  const [reviewRating, setReviewRating] = useState(5)
  const [reviewComment, setReviewComment] = useState("")
  const [submittingReview, setSubmittingReview] = useState(false)
  const [ownerReviews, setOwnerReviews] = useState<Set<number>>(new Set())

  const loadBookings = useCallback(async () => {
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
  }, [product.id, userId])

  const loadReviews = useCallback(async () => {
    try {
      setLoadingReviews(true)
      const data = await getReviewsByProduct(product.id)
      setReviews(data)
      // Track which bookings have owner reviews
      const ownerReviewedBookings = new Set<number>()
      data.forEach(r => {
        if ((r as any).reviewType === 'OWNER' && r.bookingId) {
          ownerReviewedBookings.add(r.bookingId)
        }
      })
      setOwnerReviews(ownerReviewedBookings)
    } catch {
      // Ignore review loading errors
    } finally {
      setLoadingReviews(false)
    }
  }, [product.id])

  useEffect(() => {
    void loadBookings()
    void loadReviews()
  }, [loadBookings, loadReviews])

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

  const handleRefundDeposit = async (bookingId: number) => {
    if (!confirm("Are you sure you want to refund the deposit to the customer?")) {
      return
    }

    try {
      setRefunding(bookingId)
      setError(null)
      await refundDeposit(userId, bookingId)
      await loadBookings()
      alert("Deposit refunded successfully!")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to refund deposit")
    } finally {
      setRefunding(null)
    }
  }

  const handleSubmitReview = async (bookingId: number) => {
    try {
      setSubmittingReview(true)
      setError(null)
      await createReview(userId, bookingId, reviewRating, reviewComment || undefined)
      setReviewingBooking(null)
      setReviewRating(5)
      setReviewComment("")
      await loadReviews()
      alert("Review submitted successfully!")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to submit review")
    } finally {
      setSubmittingReview(false)
    }
  }

  const handleDeleteProduct = async () => {
    if (!confirm("Are you sure you want to remove this listing? This action cannot be undone.")) {
      return
    }

    try {
      setDeleting(true)
      setError(null)
      await deleteProduct(userId, product.id)
      alert("Product removed successfully!")
      onProductDeleted?.()
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete product")
    } finally {
      setDeleting(false)
    }
  }

  const formatDate = (value: string) => {
    const d = new Date(value)
    return d.toLocaleDateString(undefined, { year: "numeric", month: "short", day: "numeric" })
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg p-6 max-w-2xl w-full shadow-xl max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold">Bookings for {product.name}</h2>
            <p className="text-sm text-gray-600">Manage reservations on this product.</p>
            {!loadingReviews && reviews.length > 0 && (
              <div className="mt-2 flex items-center gap-2">
                <span className="text-yellow-600 text-lg">⭐</span>
                <span className="text-sm font-semibold">
                  {(reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length).toFixed(1)}
                </span>
                <span className="text-sm text-gray-500">
                  ({reviews.length} review{reviews.length !== 1 ? 's' : ''})
                </span>
              </div>
            )}
          </div>
          <div className="flex gap-2">
            <Button variant="destructive" onClick={handleDeleteProduct} disabled={deleting}>
              {deleting ? "Removing..." : "Remove Listing"}
            </Button>
            <Button variant="outline" onClick={onClose}>
              Close
            </Button>
          </div>
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
          <>
            <div className="divide-y mb-6">
              {bookings.map((b) => (
                <div key={b.id} className="py-4">
                  <div className="flex items-start justify-between gap-4">
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <p className="text-sm font-semibold text-gray-900">{b.renterName}</p>
                        <span className={`px-2 py-0.5 text-xs rounded-full ${b.status === 'CANCELLED' ? 'bg-red-100 text-red-700' :
                          b.status === 'COMPLETED' ? 'bg-green-100 text-green-700' :
                            'bg-blue-100 text-blue-700'
                          }`}>
                          {b.status}
                        </span>
                        {b.depositRequested && (
                          <span className={`px-2 py-0.5 text-xs rounded-full ${b.depositPaid ? 'bg-amber-100 text-amber-700' : 'bg-orange-100 text-orange-700'
                            }`}>
                            {b.depositPaid ? '💰 Deposit Paid' : '⚠️ Deposit Requested'}
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-600">
                        {formatDate(b.bookingDate)} → {formatDate(b.returnDate)}
                      </p>
                      <p className="text-xs text-gray-500">Total: {b.totalPrice.toFixed(2)} €</p>
                      {b.depositAmount && b.depositRequested && (
                        <p className="text-xs text-amber-600">
                          Deposit: {b.depositAmount.toFixed(2)} € {b.depositReason && `- ${b.depositReason}`}
                        </p>
                      )}
                    </div>

                    <div className="flex flex-col gap-2">
                      {/* Cancel button for active bookings */}
                      {b.status !== 'CANCELLED' && b.status !== 'COMPLETED' && (
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleCancel(b.id)}
                          disabled={canceling === b.id}
                        >
                          {canceling === b.id ? "Cancelling..." : "Cancel"}
                        </Button>
                      )}

                      {/* Refund deposit button for completed bookings with deposit */}
                      {b.status === 'COMPLETED' && b.depositRequested && b.depositPaid && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleRefundDeposit(b.id)}
                          disabled={refunding === b.id}
                          className="text-green-600 border-green-600 hover:bg-green-50"
                        >
                          {refunding === b.id ? "Refunding..." : "Refund Deposit"}
                        </Button>
                      )}

                      {/* Review customer button for completed bookings */}
                      {b.status === 'COMPLETED' && !ownerReviews.has(b.id) && (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setReviewingBooking(b.id)}
                          className="text-blue-600 border-blue-600 hover:bg-blue-50"
                        >
                          Review Customer
                        </Button>
                      )}

                      {b.status === 'COMPLETED' && ownerReviews.has(b.id) && (
                        <span className="text-xs text-green-600">✓ Reviewed</span>
                      )}
                    </div>
                  </div>

                  {/* Review form */}
                  {reviewingBooking === b.id && (
                    <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                      <h4 className="text-sm font-semibold mb-3">Review Customer: {b.renterName}</h4>
                      <div className="space-y-3">
                        <div>
                          <label className="text-xs text-gray-600 block mb-1">Rating</label>
                          <div className="flex gap-1">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <button
                                key={star}
                                type="button"
                                onClick={() => setReviewRating(star)}
                                className={`text-2xl transition-colors ${star <= reviewRating ? 'text-yellow-500' : 'text-gray-300'
                                  }`}
                              >
                                ★
                              </button>
                            ))}
                          </div>
                        </div>
                        <div>
                          <label className="text-xs text-gray-600 block mb-1">Comment (optional)</label>
                          <Input
                            value={reviewComment}
                            onChange={(e) => setReviewComment(e.target.value)}
                            placeholder="Share your experience with this customer..."
                          />
                        </div>
                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            onClick={() => handleSubmitReview(b.id)}
                            disabled={submittingReview}
                          >
                            {submittingReview ? "Submitting..." : "Submit Review"}
                          </Button>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setReviewingBooking(null)
                              setReviewRating(5)
                              setReviewComment("")
                            }}
                          >
                            Cancel
                          </Button>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Reviews Section */}
            {reviews.length > 0 && (
              <div className="border-t pt-4">
                <h3 className="text-lg font-semibold mb-3">Customer Reviews</h3>
                <div className="space-y-3 max-h-60 overflow-y-auto">
                  {reviews.map((review) => (
                    <div key={review.id} className="bg-gray-50 rounded-lg p-3">
                      <div className="flex items-center gap-2 mb-1">
                        <div className="flex">
                          {Array.from({ length: 5 }).map((_, i) => (
                            <span key={i} className={i < review.rating ? "text-yellow-500" : "text-gray-300"}>
                              ★
                            </span>
                          ))}
                        </div>
                        <span className="text-xs text-gray-500">
                          {new Date(review.createdAt).toLocaleDateString()}
                        </span>
                        {(review as any).reviewType && (
                          <span className={`text-xs px-2 py-0.5 rounded-full ${(review as any).reviewType === 'OWNER'
                              ? 'bg-purple-100 text-purple-700'
                              : 'bg-blue-100 text-blue-700'
                            }`}>
                            by {(review as any).reviewType === 'OWNER' ? 'Owner' : 'Customer'}
                          </span>
                        )}
                      </div>
                      {review.comment && (
                        <p className="text-sm text-gray-700">{review.comment}</p>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}
