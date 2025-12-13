"use client"

import { useEffect, useState } from "react"
import { type Booking, getActiveBookings, cancelBooking } from "@/lib/api"

type CountdownState = {
    [bookingId: number]: string
}

function calculateCountdown(booking: Booking): { label: string; countdown: string } {
    const now = new Date()
    const bookingDate = new Date(booking.bookingDate)
    const returnDate = new Date(booking.returnDate)

    let targetDate: Date
    let label: string

    if (now < bookingDate) {
        // Booking hasn't started yet
        targetDate = bookingDate
        label = "Starts in"
    } else if (now < returnDate) {
        // Booking is currently active
        targetDate = returnDate
        label = "Ends in"
    } else {
        // Booking has ended
        return { label: "Status", countdown: "Completed" }
    }

    const diff = targetDate.getTime() - now.getTime()

    if (diff <= 0) {
        return { label, countdown: "Now" }
    }

    const days = Math.floor(diff / (1000 * 60 * 60 * 24))
    const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
    const seconds = Math.floor((diff % (1000 * 60)) / 1000)

    let countdown = ""
    if (days > 0) countdown += `${days}d `
    if (hours > 0 || days > 0) countdown += `${hours}h `
    if (minutes > 0 || hours > 0 || days > 0) countdown += `${minutes}m `
    countdown += `${seconds}s`

    return { label, countdown: countdown.trim() }
}

export default function ActiveBookingsPage() {
    const [bookings, setBookings] = useState<Booking[]>([])
    const [countdowns, setCountdowns] = useState<CountdownState>({})
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [cancellingId, setCancellingId] = useState<number | null>(null)

    // TODO: Replace with actual user ID from auth context
    const userId = 1

    useEffect(() => {
        async function fetchBookings() {
            try {
                setLoading(true)
                const data = await getActiveBookings(userId)
                setBookings(data)
                setError(null)
            } catch (err) {
                setError(err instanceof Error ? err.message : "Failed to load bookings")
            } finally {
                setLoading(false)
            }
        }

        fetchBookings()
    }, [userId])

    // Update countdowns every second
    useEffect(() => {
        if (bookings.length === 0) return

        const updateCountdowns = () => {
            const newCountdowns: CountdownState = {}
            bookings.forEach((booking) => {
                const { countdown } = calculateCountdown(booking)
                newCountdowns[booking.id] = countdown
            })
            setCountdowns(newCountdowns)
        }

        // Initial update
        updateCountdowns()

        // Set interval for updates
        const interval = setInterval(updateCountdowns, 1000)

        return () => clearInterval(interval)
    }, [bookings])

    const handleCancel = async (bookingId: number) => {
        if (!confirm("Are you sure you want to cancel this booking?")) return

        try {
            setCancellingId(bookingId)
            await cancelBooking(userId, bookingId)
            // Remove from list after cancellation
            setBookings((prev) => prev.filter((b) => b.id !== bookingId))
        } catch (err) {
            alert(err instanceof Error ? err.message : "Failed to cancel booking")
        } finally {
            setCancellingId(null)
        }
    }

    const canCancel = (booking: Booking): boolean => {
        const now = new Date()
        const bookingDate = new Date(booking.bookingDate)
        return now < bookingDate // Can only cancel if booking hasn't started
    }

    if (loading) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-lg">Loading active bookings...</div>
            </div>
        )
    }

    if (error) {
        return (
            <div className="flex items-center justify-center min-h-screen">
                <div className="text-red-500">{error}</div>
            </div>
        )
    }

    return (
        <div className="container mx-auto px-4 py-8">
            <h1 className="text-3xl font-bold mb-8">Active Bookings</h1>

            {bookings.length === 0 ? (
                <div className="text-center text-gray-500 py-12">
                    <p className="text-xl">No active bookings</p>
                    <p className="mt-2">Your active rentals will appear here</p>
                </div>
            ) : (
                <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                    {bookings.map((booking) => {
                        const { label } = calculateCountdown(booking)
                        const countdown = countdowns[booking.id] || "..."

                        return (
                            <div
                                key={booking.id}
                                className="bg-white rounded-lg shadow-md p-6 border border-gray-200"
                            >
                                <h2 className="text-xl font-semibold mb-2">{booking.productName}</h2>
                                <p className="text-gray-600 mb-1">Owner: {booking.ownerName}</p>
                                <p className="text-gray-600 mb-1">
                                    From: {new Date(booking.bookingDate).toLocaleDateString()}
                                </p>
                                <p className="text-gray-600 mb-1">
                                    To: {new Date(booking.returnDate).toLocaleDateString()}
                                </p>
                                <p className="text-gray-600 mb-4">
                                    Total: €{booking.totalPrice.toFixed(2)}
                                </p>

                                <div className="bg-blue-50 rounded-lg p-4 mb-4">
                                    <p className="text-sm text-blue-600 font-medium">{label}</p>
                                    <p className="text-2xl font-bold text-blue-700">{countdown}</p>
                                </div>

                                {canCancel(booking) && (
                                    <button
                                        onClick={() => handleCancel(booking.id)}
                                        disabled={cancellingId === booking.id}
                                        className="w-full bg-red-500 hover:bg-red-600 text-white font-medium py-2 px-4 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                                    >
                                        {cancellingId === booking.id ? "Cancelling..." : "Cancel Booking"}
                                    </button>
                                )}
                            </div>
                        )
                    })}
                </div>
            )}
        </div>
    )
}
