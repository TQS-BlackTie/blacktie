type Review = {
  id: number
  bookingId: number
  rating: number
  comment?: string
  createdAt: string
}

type Props = {
  review: Review
  onClose: () => void
}

export default function ReviewDisplay({ review, onClose }: Props) {
  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center text-black z-50">
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-md p-6 z-50">
        <h2 className="text-lg font-bold mb-2">Your Review</h2>
        <p className="text-sm text-muted-foreground mb-4">Submitted: {new Date(review.createdAt).toLocaleString()}</p>

        <div className="space-y-3">
          <div>
            <p className="text-sm font-medium">Rating</p>
            <div className="text-xl font-semibold">{review.rating} / 5</div>
          </div>
          {review.comment && (
            <div>
              <p className="text-sm font-medium">Comment</p>
              <p className="text-sm">{review.comment}</p>
            </div>
          )}
        </div>

        <div className="flex justify-end mt-6">
          <button className="px-4 py-2 border rounded" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
