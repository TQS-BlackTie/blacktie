"use client"

import { useEffect, useState } from "react"
import { type Booking, getActiveBookings, getRenterHistory, cancelBooking } from "@/lib/api"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Button } from "@/components/ui/button"

type Tab = "active" | "history"

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
        targetDate = bookingDate
        label = "Starts in"
    } else if (now < returnDate) {
        targetDate = returnDate
        label = "Ends in"
    } else {
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

export default function MyBookingsPage() {
    const [activeTab, setActiveTab] = useState<Tab>("active")
    const [activeBookings, setActiveBookings] = useState<Booking[]>([])
    const [historyBookings, setHistoryBookings] = useState<Booking[]>([])
    const [countdowns, setCountdowns] = useState<CountdownState>({})
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)
    const [cancellingId, setCancellingId] = useState<number | null>(null)

    const userId = (() => {
        const userData = localStorage.getItem('user')
        if (userData) {
            try {
                return JSON.parse(userData).id
            } catch {
                return null
            }
        }
        return null
    })()

    useEffect(() => {
        async function fetchData() {
            if (!userId) {
                window.location.href = '/login'
                return
            }

            try {
                setLoading(true)
                const [active, history] = await Promise.all([
                    getActiveBookings(userId),
                    getRenterHistory(userId)
                ])
                setActiveBookings(active)
                setHistoryBookings(history)
                setError(null)
            } catch (err) {
                setError(err instanceof Error ? err.message : "Failed to load bookings")
            } finally {
                setLoading(false)
            }
        }

        fetchData()
    }, [userId])

    // Update countdowns every second
    useEffect(() => {
        if (activeBookings.length === 0) return

        const updateCountdowns = () => {
            const newCountdowns: CountdownState = {}
            activeBookings.forEach((booking) => {
                const { countdown } = calculateCountdown(booking)
                newCountdowns[booking.id] = countdown
            })
            setCountdowns(newCountdowns)
        }

        updateCountdowns()
        const interval = setInterval(updateCountdowns, 1000)
        return () => clearInterval(interval)
    }, [activeBookings])

    const handleCancel = async (bookingId: number) => {
        if (!confirm("Are you sure you want to cancel this booking?")) return

        try {
            setCancellingId(bookingId)
            await cancelBooking(userId, bookingId)
            setActiveBookings((prev) => prev.filter((b) => b.id !== bookingId))
        } catch (err) {
            alert(err instanceof Error ? err.message : "Failed to cancel booking")
        } finally {
            setCancellingId(null)
        }
    }

    const canCancel = (booking: Booking): boolean => {
        const now = new Date()
        const bookingDate = new Date(booking.bookingDate)
        return now < bookingDate
    }

    const formatDate = (dateString: string) => {
        return new Date(dateString).toLocaleDateString('en-GB', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        })
    }

    const getStatusBadge = (status: string) => {
        const statusColors: Record<string, string> = {
            COMPLETED: 'bg-green-100 text-green-800',
            CANCELLED: 'bg-red-100 text-red-800',
            ACTIVE: 'bg-blue-100 text-blue-800'
        }
        return statusColors[status] || 'bg-gray-100 text-gray-800'
    }

    const handleBack = () => {
        window.location.href = '/'
    }

    if (loading) {
        return (
            <div className="flex min-h-screen items-center justify-center p-6">
                <div>Loading your bookings...</div>
            </div>
        )
    }

    return (
        <div className="min-h-screen p-6 bg-slate-50">
            <div className="max-w-5xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h1 className="text-2xl font-bold">My Bookings</h1>
                        <p className="text-sm text-muted-foreground">
                            Manage your active rentals and view history
                        </p>
                    </div>
                    <Button onClick={handleBack} variant="outline">
                        Back to Home
                    </Button>
                </div>

                {/* Tabs */}
                <div className="flex gap-2 mb-6">
                    <button
                        onClick={() => setActiveTab("active")}
                        className={`px-4 py-2 rounded-lg font-medium transition-colors ${activeTab === "active"
                                ? "bg-blue-500 text-white"
                                : "bg-white text-gray-600 hover:bg-gray-100 border border-gray-200"
                            }`}
                    >
                        Active ({activeBookings.length})
                    </button>
                    <button
                        onClick={() => setActiveTab("history")}
                        className={`px-4 py-2 rounded-lg font-medium transition-colors ${activeTab === "history"
                                ? "bg-blue-500 text-white"
                                : "bg-white text-gray-600 hover:bg-gray-100 border border-gray-200"
                            }`}
                    >
                        History ({historyBookings.length})
                    </button>
                </div>

                {error && (
                    <div className="text-red-600 bg-red-50 p-4 rounded-lg mb-4">
                        {error}
                    </div>
                )}

                {/* Active Bookings Tab */}
                {activeTab === "active" && (
                    <>
                        {activeBookings.length === 0 ? (
                            <Card>
                                <CardContent className="pt-6">
                                    <p className="text-center text-muted-foreground">
                                        No active bookings. Browse the catalog to rent something!
                                    </p>
                                </CardContent>
                            </Card>
                        ) : (
                            <div className="grid gap-4 md:grid-cols-2">
                                {activeBookings.map((booking) => {
                                    const { label } = calculateCountdown(booking)
                                    const countdown = countdowns[booking.id] || "..."

                                    return (
                                        <Card key={booking.id} className="hover:shadow-md transition-shadow">
                                            <CardHeader>
                                                <div className="flex items-start justify-between">
                                                    <div>
                                                        <CardTitle className="text-lg">{booking.productName}</CardTitle>
                                                        <p className="text-sm text-muted-foreground">
                                                            Owner: {booking.ownerName || 'Unknown'}
                                                        </p>
                                                    </div>
                                                    <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusBadge(booking.status)}`}>
                                                        {booking.status}
                                                    </span>
                                                </div>
                                            </CardHeader>
                                            <CardContent>
                                                <div className="grid grid-cols-2 gap-4 mb-4">
                                                    <div>
                                                        <p className="text-sm font-medium text-muted-foreground">From</p>
                                                        <p className="text-sm">{formatDate(booking.bookingDate)}</p>
                                                    </div>
                                                    <div>
                                                        <p className="text-sm font-medium text-muted-foreground">To</p>
                                                        <p className="text-sm">{formatDate(booking.returnDate)}</p>
                                                    </div>
                                                </div>

                                                {/* Countdown */}
                                                <div className="bg-blue-50 rounded-lg p-3 mb-4">
                                                    <p className="text-xs text-blue-600 font-medium">{label}</p>
                                                    <p className="text-xl font-bold text-blue-700">{countdown}</p>
                                                </div>

                                                <div className="flex items-center justify-between">
                                                    <p className="text-sm font-bold">€{booking.totalPrice.toFixed(2)}</p>
                                                    {canCancel(booking) && (
                                                        <Button
                                                            onClick={() => handleCancel(booking.id)}
                                                            disabled={cancellingId === booking.id}
                                                            variant="destructive"
                                                            size="sm"
                                                        >
                                                            {cancellingId === booking.id ? "Cancelling..." : "Cancel"}
                                                        </Button>
                                                    )}
                                                </div>
                                            </CardContent>
                                        </Card>
                                    )
                                })}
                            </div>
                        )}
                    </>
                )}

                {/* History Tab */}
                {activeTab === "history" && (
                    <>
                        {historyBookings.length === 0 ? (
                            <Card>
                                <CardContent className="pt-6">
                                    <p className="text-center text-muted-foreground">
                                        No booking history yet. Complete a rental to see it here!
                                    </p>
                                </CardContent>
                            </Card>
                        ) : (
                            <div className="space-y-4">
                                {historyBookings.map((booking) => (
                                    <Card key={booking.id} className="hover:shadow-md transition-shadow">
                                        <CardHeader>
                                            <div className="flex items-start justify-between">
                                                <div>
                                                    <CardTitle className="text-lg">{booking.productName}</CardTitle>
                                                    <p className="text-sm text-muted-foreground">
                                                        Owner: {booking.ownerName || 'Unknown'}
                                                    </p>
                                                </div>
                                                <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusBadge(booking.status)}`}>
                                                    {booking.status}
                                                </span>
                                            </div>
                                        </CardHeader>
                                        <CardContent>
                                            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                                <div>
                                                    <p className="text-sm font-medium text-muted-foreground">From</p>
                                                    <p className="text-sm">{formatDate(booking.bookingDate)}</p>
                                                </div>
                                                <div>
                                                    <p className="text-sm font-medium text-muted-foreground">To</p>
                                                    <p className="text-sm">{formatDate(booking.returnDate)}</p>
                                                </div>
                                                <div>
                                                    <p className="text-sm font-medium text-muted-foreground">Total Paid</p>
                                                    <p className="text-sm font-bold">€{booking.totalPrice.toFixed(2)}</p>
                                                </div>
                                            </div>
                                        </CardContent>
                                    </Card>
                                ))}
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    )
}
