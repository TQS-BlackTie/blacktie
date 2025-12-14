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
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-md p-6 z-50">
        <h2 className="text-lg font-bold mb-2 text-slate-900">Your Review</h2>
        <p className="text-sm text-slate-500 mb-4">Submitted: {new Date(review.createdAt).toLocaleString()}</p>

        <div className="space-y-3">
          <div>
            <p className="text-sm font-medium text-slate-700">Rating</p>
            <div className="text-xl font-semibold text-slate-900">{review.rating} / 5</div>
          </div>
          {review.comment && (
            <div>
              <p className="text-sm font-medium text-slate-700">Comment</p>
              <p className="text-sm text-slate-600">{review.comment}</p>
            </div>
          )}
        </div>

        <div className="flex justify-end mt-6">
          <button className="px-4 py-2 border border-slate-300 text-slate-700 rounded hover:bg-slate-50" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
