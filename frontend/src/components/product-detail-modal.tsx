import { useEffect, useState } from "react"
import { getReviewsByProduct, type Product, type ReviewResponse } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Info } from "lucide-react"

type ProductDetailModalProps = {
  product: Product
  userId: number
  rating?: { avg: number; count: number }
  onClose: () => void
  onReserve: () => void
  onViewProfile?: (userId: number) => void
}

export function ProductDetailModal({
  product,
  userId,
  rating,
  onClose,
  onReserve,
  onViewProfile,
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

  // Calculate total at checkout (rental + deposit)
  const hasDeposit = product.depositAmount && product.depositAmount > 0

  return (
    <div
      className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-[100]"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <div
        className="bg-white rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden shadow-2xl flex flex-col md:flex-row"
      >
        {/* Left side - Image */}
        <div className="md:w-1/2 relative bg-gray-100 flex-shrink-0">
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
                  <div className="flex items-center gap-2 mt-1">
                    <p className="text-sm text-slate-500">
                      by {product.owner.name}
                    </p>
                    {onViewProfile && product.owner.id && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-6 w-6 text-gray-500 hover:text-blue-600"
                        onClick={() => onViewProfile(product.owner!.id)}
                        title={`View ${product.owner.name}'s profile`}
                      >
                        <Info size={16} />
                      </Button>
                    )}
                  </div>
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

            {/* Deposit Info */}
            {hasDeposit && (
              <div className="mt-3 p-3 bg-amber-50 border border-amber-200 rounded-lg">
                <div className="flex items-center gap-2">
                  <svg className="w-5 h-5 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  <span className="font-semibold text-amber-800">
                    Security Deposit: {product.depositAmount!.toFixed(2)} €
                  </span>
                </div>
                <p className="text-sm text-amber-700 mt-1">
                  A refundable deposit may be requested by the owner to cover potential damages. This will be returned after the item is returned in good condition.
                </p>
              </div>
            )}
          </div>

          {/* Description */}
          <div className="p-6 border-b border-gray-100">
            <h3 className="font-semibold text-slate-700 mb-2">Description</h3>
            <p className="text-slate-600">{product.description || "No description available."}</p>
            {product.size && (
              <div className="mt-3 flex items-center gap-2 text-slate-600">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
                </svg>
                <span className="font-medium">Size:</span>
                <span className="bg-slate-100 px-3 py-1 rounded-full text-sm font-semibold">{product.size}</span>
              </div>
            )}
          </div>

          {/* Location */}
          {(product.address || product.city || product.postalCode) && (
            <div className="p-6 border-b border-gray-100">
              <h3 className="font-semibold text-slate-700 mb-2 flex items-center gap-2">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
                Location
              </h3>
              <div className="text-slate-600 space-y-1">
                {product.address && <p>{product.address}</p>}
                {(product.city || product.postalCode) && (
                  <p>{[product.postalCode, product.city].filter(Boolean).join(" ")}</p>
                )}
                {product.latitude && product.longitude && (
                  <p className="text-sm text-slate-500">
                    Coordinates: {product.latitude.toFixed(6)}, {product.longitude.toFixed(6)}
                  </p>
                )}
              </div>
            </div>
          )}

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
                            className={`text-lg ${star <= review.rating ? "text-yellow-400" : "text-slate-300"
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
            {/* Price Summary */}
            {hasDeposit && (
              <div className="mb-4 text-sm text-slate-600">
                <div className="flex justify-between">
                  <span>Rental (per day)</span>
                  <span>{product.price.toFixed(2)} €</span>
                </div>
                <div className="flex justify-between text-amber-700">
                  <span>Security Deposit (refundable)</span>
                  <span>+{product.depositAmount!.toFixed(2)} €</span>
                </div>
                <div className="border-t mt-2 pt-2 flex justify-between font-semibold text-slate-900">
                  <span>Total at checkout (1 day)</span>
                  <span>{(product.price + product.depositAmount!).toFixed(2)} €</span>
                </div>
              </div>
            )}
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