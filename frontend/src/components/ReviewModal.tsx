import { useState } from 'react'
import { createReview } from '@/lib/api'

type Props = {
  bookingId: number
  userId: number
  onClose: () => void
  onSuccess: (review: any) => void
}

export default function ReviewModal({ bookingId, userId, onClose, onSuccess }: Props) {
  const [rating, setRating] = useState<number>(5)
  const [comment, setComment] = useState<string>('')
  
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const submit = async () => {
    setError('')
    setLoading(true)
    try {
      const review = await createReview(userId, bookingId, rating, comment || undefined)
      onSuccess(review)
      onClose()
    } catch (e: any) {
      setError(e.message || 'Failed to submit review')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center">
      <div className="bg-white rounded shadow-lg w-full max-w-lg p-6">
        <h2 className="text-lg font-bold mb-4">Leave a Review</h2>
        {error && <div className="text-red-600 mb-2">{error}</div>}
        <div className="space-y-3">
          <div>
            <label className="block text-sm">Rating</label>
            <select value={rating} onChange={e => setRating(Number(e.target.value))} className="border p-2 w-24">
              {[5,4,3,2,1].map(n => <option key={n} value={n}>{n}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm">Comment (optional)</label>
            <textarea value={comment} onChange={e => setComment(e.target.value)} className="w-full border p-2 h-24" />
          </div>
          
        </div>

        <div className="flex justify-end gap-2 mt-6">
          <button className="px-4 py-2 border rounded" onClick={onClose} disabled={loading}>Cancel</button>
          <button className="px-4 py-2 bg-blue-600 text-white rounded" onClick={submit} disabled={loading}>{loading ? 'Saving...' : 'Submit'}</button>
        </div>
      </div>
    </div>
  )
}
