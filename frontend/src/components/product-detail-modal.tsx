import { useEffect, useState } from "react"
import { getReviewsByProduct, type Product, type ReviewResponse } from "@/lib/api"
import { Button } from "@/components/ui/button"

type ProductDetailModalProps = {
  product: Product
  userId: number
  rating?: { avg: number; count: number }
  onClose: () => void
  onReserve: () => void
}

export function ProductDetailModal({
  product,
  userId,
  rating,
  onClose,
  onReserve,
}: ProductDetailModalProps) {
  const [reviews, setReviews] = useState<ReviewResponse[]>([])
  const [loadingReviews, setLoadingReviews] = useState(false)

  useEffect(() => {
    const loadReviews = async () => {
      try {
        setLoadingReviews(true)
        const data = await getReviewsByProduct(product.id)
        setReviews(data)
      } catch {
        // ignore errors
      } finally {
        setLoadingReviews(false)
      }
    }

    void loadReviews()
  }, [product.id])

  const isOwnProduct = product.owner?.id === userId
  const canReserve = product.available && !isOwnProduct

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden shadow-2xl flex flex-col md:flex-row">
        {/* Left side - Image */}
        <div className="md:w-1/2 relative bg-gray-100">
          {product.imageUrl ? (
            <img
              src={product.imageUrl}
              alt={product.name}
              className="w-full h-64 md:h-full object-cover"
            />
          ) : (
            <div className="w-full h-64 md:h-full flex items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200">
              <svg
                className="w-24 h-24 text-slate-300"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                />
              </svg>
            </div>
          )}
          {!product.available && (
            <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
              <span className="text-white font-semibold px-4 py-2 bg-red-500 rounded-full">
                Currently Unavailable
              </span>
            </div>
          )}
        </div>

        {/* Right side - Info */}
        <div className="md:w-1/2 flex flex-col overflow-hidden">
          {/* Header */}
          <div className="p-6 border-b border-gray-100">
            <div className="flex items-start justify-between">
              <div>
                <h2 className="text-2xl font-bold text-slate-900">{product.name}</h2>
                {product.owner?.name && (
                  <p className="text-sm text-slate-500 mt-1">
                    by {product.owner.name}
                  </p>
                )}
              </div>
              <button
                onClick={onClose}
                className="text-slate-400 hover:text-slate-600 transition"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div className="flex items-center gap-4 mt-4">
              <span className="text-3xl font-bold text-emerald-600">
                {product.price.toFixed(2)} €
                <span className="text-sm font-normal text-slate-500"> / day</span>
              </span>
              {rating && rating.count > 0 && (
                <span className="flex items-center gap-1 text-yellow-500 font-medium">
                  ⭐ {rating.avg.toFixed(1)}
                  <span className="text-slate-400 text-sm">({rating.count})</span>
                </span>
              )}
            </div>
          </div>

          {/* Description */}
          <div className="p-6 border-b border-gray-100">
            <h3 className="font-semibold text-slate-700 mb-2">Description</h3>
            <p className="text-slate-600">{product.description || "No description available."}</p>
          </div>

          {/* Reviews */}
          <div className="flex-1 p-6 overflow-y-auto">
            <h3 className="font-semibold text-slate-700 mb-3">
              Reviews {reviews.length > 0 && `(${reviews.length})`}
            </h3>
            
            {loadingReviews ? (
              <p className="text-slate-500 text-sm">Loading reviews...</p>
            ) : reviews.length === 0 ? (
              <p className="text-slate-500 text-sm">No reviews yet.</p>
            ) : (
              <div className="space-y-4">
                {reviews.map((review) => (
                  <div
                    key={review.id}
                    className="bg-slate-50 rounded-lg p-4"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-1">
                        {[1, 2, 3, 4, 5].map((star) => (
                          <span
                            key={star}
                            className={`text-lg ${
                              star <= review.rating ? "text-yellow-400" : "text-slate-300"
                            }`}
                          >
                            ★
                          </span>
                        ))}
                      </div>
                      <span className="text-xs text-slate-400">
                        {new Date(review.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    {review.comment && (
                      <p className="text-slate-600 text-sm">{review.comment}</p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer Actions */}
          <div className="p-6 border-t border-gray-100 bg-slate-50">
            <div className="flex gap-3">
              <Button
                variant="outline"
                onClick={onClose}
                className="flex-1"
              >
                Close
              </Button>
              <Button
                onClick={onReserve}
                disabled={!canReserve}
                className="flex-1"
              >
                {isOwnProduct
                  ? "Your listing"
                  : product.available
                    ? "Reserve Now"
                    : "Unavailable"}
              </Button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
