import { useEffect, useMemo, useState } from "react"
import { createBooking, getBookingsByProduct, type Booking, type Product } from "@/lib/api"
import { Button } from "@/components/ui/button"

type DateRange = {
  start: string | null
  end: string | null
}

type DisabledRange = {
  start: Date
  end: Date
}

const formatKey = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

const parseDateKey = (dateKey: string) => {
  const [year, month, day] = dateKey.split("-").map(Number)
  return new Date(year, month - 1, day)
}

const startOfDay = (date: Date) => new Date(date.getFullYear(), date.getMonth(), date.getDate())

const addDays = (date: Date, days: number) => {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + days)
  return copy
}

type BookingModalProps = {
  product: Product
  userId: number
  onClose: () => void
  onSuccess: () => void
}

export function BookingModal({ product, userId, onClose, onSuccess }: BookingModalProps) {
  const [range, setRange] = useState<DateRange>({ start: null, end: null })
  const [viewMonth, setViewMonth] = useState<Date>(() => new Date())
  const [productBookings, setProductBookings] = useState<Booking[]>([])
  const [calendarLoading, setCalendarLoading] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const today = useMemo(() => startOfDay(new Date()), [])

  useEffect(() => {
    const loadBookings = async () => {
      try {
        setCalendarLoading(true)
        setError(null)
        const bookings = await getBookingsByProduct(product.id, userId)
        setProductBookings(bookings)
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load existing bookings")
      } finally {
        setCalendarLoading(false)
      }
    }

    void loadBookings()
  }, [product.id, userId])

  const disabledRanges: DisabledRange[] = useMemo(
    () =>
      productBookings
        .filter((b) => b.status !== 'CANCELLED' && b.status !== 'COMPLETED')
        .map((b) => ({
          start: parseDateKey(b.bookingDate.split("T")[0]),
          end: parseDateKey(b.returnDate.split("T")[0]),
        })),
    [productBookings],
  )

  const isDateBlocked = (date: Date) => {
    if (date <= today) return true
    return disabledRanges.some((range) => date >= range.start && date <= range.end)
  }

  const rangeCrossesBlockedDay = (start: Date, end: Date) => {
    let cursor = startOfDay(start)
    while (cursor <= end) {
      if (isDateBlocked(cursor)) return true
      cursor = addDays(cursor, 1)
    }
    return false
  }

  const totalPrice = useMemo(() => {
    if (!range.start || !range.end) return null
    const start = parseDateKey(range.start)
    const end = parseDateKey(range.end)
    const diffDays = Math.round((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
    return diffDays > 0 ? diffDays * product.price : null
  }, [product.price, range.end, range.start])

  const handleDayClick = (date: Date) => {
    if (isDateBlocked(date)) return
    setError(null)

    const key = formatKey(date)

    if (range.start && !range.end && key === range.start) {
      setRange({ start: null, end: null })
      return
    }

    if (range.start && range.end && (key === range.start || key === range.end)) {
      setRange({ start: null, end: null })
      return
    }

    if (!range.start || range.end) {
      setRange({ start: formatKey(date), end: null })
      setViewMonth(date)
      return
    }

    const start = parseDateKey(range.start)
    if (date <= start) {
      setRange({ start: formatKey(date), end: null })
      setViewMonth(date)
      return
    }

    if (rangeCrossesBlockedDay(start, date)) {
      setError("Selected range overlaps an existing booking")
      return
    }

    setRange({ start: formatKey(start), end: formatKey(date) })
  }

  const toLocalDateTimeString = (dateKey: string) => `${dateKey}T00:00:00`

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (!range.start || !range.end) {
      setError("Please select both dates")
      return
    }

    const start = parseDateKey(range.start)
    const end = parseDateKey(range.end)

    if (end <= start) {
      setError("Return date must be after booking date")
      return
    }

    if (rangeCrossesBlockedDay(start, end)) {
      setError("Selected dates overlap with an existing reservation")
      return
    }

    if (start < today) {
      setError("Booking date cannot be in the past")
      return
    }

    try {
      setLoading(true)
      await createBooking(userId, {
        productId: product.id,
        bookingDate: toLocalDateTimeString(range.start),
        returnDate: toLocalDateTimeString(range.end),
      })
      setSuccess(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create booking")
    } finally {
      setLoading(false)
    }
  }

  const renderCalendarDays = () => {
    const startMonth = new Date(viewMonth.getFullYear(), viewMonth.getMonth(), 1)
    const startWeekDay = startMonth.getDay()
    const daysInMonth = new Date(viewMonth.getFullYear(), viewMonth.getMonth() + 1, 0).getDate()
    const prevMonthDays = new Date(viewMonth.getFullYear(), viewMonth.getMonth(), 0).getDate()

    const cells: Date[] = []

    for (let i = startWeekDay; i > 0; i--) {
      cells.push(
        new Date(viewMonth.getFullYear(), viewMonth.getMonth() - 1, prevMonthDays - i + 1),
      )
    }

    for (let day = 1; day <= daysInMonth; day++) {
      cells.push(new Date(viewMonth.getFullYear(), viewMonth.getMonth(), day))
    }

    while (cells.length % 7 !== 0) {
      const last = cells[cells.length - 1]
      cells.push(addDays(last, 1))
    }

    const isSelected = (date: Date) => {
      if (!range.start) return false
      const start = parseDateKey(range.start)
      const end = range.end ? parseDateKey(range.end) : null
      if (end) {
        return date >= start && date <= end
      }
      return formatKey(date) === formatKey(start)
    }

    const isEdge = (date: Date) => {
      if (!range.start) return false
      const start = parseDateKey(range.start)
      const end = range.end ? parseDateKey(range.end) : null
      return (
        formatKey(date) === formatKey(start) ||
        (end !== null && formatKey(date) === formatKey(end))
      )
    }

    return (
      <div className="grid grid-cols-7 gap-1 text-sm">
        {["S", "M", "T", "W", "T", "F", "S"].map((d) => (
          <div key={d} className="text-center text-gray-500 py-1">
            {d}
          </div>
        ))}
        {cells.map((date) => {
          const key = formatKey(date)
          const inCurrentMonth = date.getMonth() === viewMonth.getMonth()
          const blocked = isDateBlocked(date)
          const selected = isSelected(date)
          const edge = isEdge(date)

          const baseClasses =
            "w-9 h-9 rounded-full flex items-center justify-center border transition-colors"
          const monthClasses = inCurrentMonth ? "text-gray-900" : "text-gray-400"
          const blockedClasses = blocked
            ? "bg-gray-200 text-gray-500 cursor-not-allowed border-gray-200"
            : "hover:bg-blue-50 border-transparent cursor-pointer"
          const selectedClasses = selected
            ? edge
              ? "bg-blue-600 text-white border-blue-600"
              : "bg-blue-100 text-blue-700 border-blue-100"
            : ""

          return (
            <button
              key={key}
              type="button"
              onClick={() => handleDayClick(date)}
              disabled={blocked}
              className={`${baseClasses} ${monthClasses} ${blockedClasses} ${selectedClasses}`}
            >
              {date.getDate()}
            </button>
          )
        })}
      </div>
    )
  }

  const moveMonth = (delta: number) => {
    setViewMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + delta, 1))
  }

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg p-6 max-w-md w-full shadow-xl">
        {success ? (
          <div className="space-y-4">
            <div className="text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-green-100 mb-4">
                <svg className="h-6 w-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 className="text-2xl font-bold mb-2">Booking Request Submitted!</h2>
              <p className="text-gray-600 mb-4">
                Your booking request has been sent to the owner for approval.
              </p>
              <div className="bg-blue-50 p-4 rounded-lg text-left">
                <p className="text-sm text-blue-800 mb-2">
                  <strong>Next steps:</strong>
                </p>
                <ol className="text-sm text-blue-700 space-y-1 list-decimal list-inside">
                  <li>Wait for the owner to review your request</li>
                  <li>You'll receive a notification when approved</li>
                  <li>Once approved, you can proceed with payment in "My Bookings"</li>
                </ol>
              </div>
            </div>
            <Button
              onClick={() => {
                onSuccess()
                onClose()
              }}
              className="w-full"
            >
              Go to My Bookings
            </Button>
          </div>
        ) : (
          <>
            <h2 className="text-2xl font-bold mb-4">Reserve {product.name}</h2>
        
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

          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium">Select booking range</p>
              </div>
              <div className="flex items-center gap-2">
                <Button type="button" size="sm" variant="outline" onClick={() => moveMonth(-1)}>
                  ‹
                </Button>
                <span className="text-sm font-semibold">
                  {viewMonth.toLocaleString(undefined, { month: "long", year: "numeric" })}
                </span>
                <Button type="button" size="sm" variant="outline" onClick={() => moveMonth(1)}>
                  ›
                </Button>
              </div>
            </div>

            <div className="rounded-lg border p-3 bg-gray-50">
              {calendarLoading ? (
                <p className="text-sm text-gray-500">Loading calendar…</p>
              ) : (
                renderCalendarDays()
              )}
            </div>

            <div className="flex items-center gap-2 text-sm text-gray-700">
              <span className="font-medium">Selected:</span>
              <span>
                {range.start
                  ? `${range.start}${range.end ? ` → ${range.end}` : ""}`
                  : "No dates chosen"}
              </span>
            </div>
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
              {loading ? "Submitting..." : "Submit Booking Request"}
            </Button>
          </div>
        </form>
          </>
        )}
      </div>
    </div>
  )
}
