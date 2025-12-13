"use client"

import { useEffect, useState } from "react"
import { type Booking, getActiveBookings, getRenterHistory, cancelBooking } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { Navbar } from "@/components/navbar"
import { NotificationBell } from "@/components/notification-bell"

type Tab = "active" | "history"

type CountdownState = {
    [bookingId: number]: string
}

interface User {
    id: number
    name: string
    email: string
    role: string
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

    const [user] = useState<User | null>(() => {
        if (typeof window === 'undefined') return null
        const userData = window.localStorage.getItem('user')
        if (!userData) return null
        try {
            return JSON.parse(userData) as User
        } catch (error) {
            console.error('Failed to parse user data from localStorage', error)
            return null
        }
    })

    const userId = user?.id || null

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
        if (!userId) return

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

    const handleLogout = () => {
        localStorage.removeItem('user')
        window.location.href = '/login'
    }

    if (!user) {
        return <div className="flex min-h-screen items-center justify-center">Loading...</div>
    }

    if (loading) {
        return (
            <div className="flex min-h-screen items-center justify-center p-6">
                <div>Loading your bookings...</div>
            </div>
        )
    }

    return (
        <div className="min-h-screen bg-slate-50">
            <Navbar
                userName={user.name}
                userRole={user.role}
                onLogout={handleLogout}
                notificationBell={<NotificationBell userId={user.id} />}
            />

            <main className="relative z-10">
                <section className="w-full px-6 pb-12 mt-8 md:px-12 lg:px-20">
                    <div className="max-w-7xl mx-auto">
                        {/* Header */}
                        <div className="mb-8">
                            <h1 className="text-3xl font-bold text-slate-900">My Bookings</h1>
                            <p className="text-slate-600 mt-2">
                                Manage your active rentals and view history
                            </p>
                        </div>

                        {/* Tabs */}
                        <div className="flex gap-2 mb-6">
                            <button
                                onClick={() => setActiveTab("active")}
                                className={`px-6 py-3 rounded-full font-medium transition-all duration-200 ${activeTab === "active"
                                        ? "bg-gradient-to-r from-emerald-400 via-cyan-400 to-blue-500 text-white shadow-lg shadow-emerald-900/20"
                                        : "bg-white text-slate-600 hover:bg-slate-100 border border-slate-200"
                                    }`}
                            >
                                Active ({activeBookings.length})
                            </button>
                            <button
                                onClick={() => setActiveTab("history")}
                                className={`px-6 py-3 rounded-full font-medium transition-all duration-200 ${activeTab === "history"
                                        ? "bg-gradient-to-r from-emerald-400 via-cyan-400 to-blue-500 text-white shadow-lg shadow-emerald-900/20"
                                        : "bg-white text-slate-600 hover:bg-slate-100 border border-slate-200"
                                    }`}
                            >
                                History ({historyBookings.length})
                            </button>
                        </div>

                        {error && (
                            <div className="text-red-600 bg-red-50 p-4 rounded-lg mb-4 border border-red-200">
                                {error}
                            </div>
                        )}

                {/* Active Bookings Tab */}
                {activeTab === "active" && (
                    <>
                        {activeBookings.length === 0 ? (
                            <div className="rounded-3xl border border-white/15 bg-white/75 p-8 text-center shadow-2xl backdrop-blur">
                                <p className="text-slate-600">
                                    No active bookings. Browse the catalog to rent something!
                                </p>
                            </div>
                        ) : (
                            <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                                {activeBookings.map((booking) => {
                                    const { label } = calculateCountdown(booking)
                                    const countdown = countdowns[booking.id] || "..."

                                    return (
                                        <div
                                            key={booking.id}
                                            className="group rounded-3xl border border-white/15 bg-white/75 p-6 shadow-2xl backdrop-blur transition-all duration-300 hover:shadow-emerald-500/10 hover:-translate-y-1"
                                        >
                                            <div className="flex items-start justify-between mb-4">
                                                <div>
                                                    <h3 className="text-lg font-bold text-slate-900">{booking.productName}</h3>
                                                    <p className="text-sm text-slate-600">
                                                        Owner: {booking.ownerName || 'Unknown'}
                                                    </p>
                                                </div>
                                                <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusBadge(booking.status)}`}>
                                                    {booking.status}
                                                </span>
                                            </div>

                                            <div className="grid grid-cols-2 gap-4 mb-4">
                                                <div>
                                                    <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">From</p>
                                                    <p className="text-sm font-semibold text-slate-900">{formatDate(booking.bookingDate)}</p>
                                                </div>
                                                <div>
                                                    <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">To</p>
                                                    <p className="text-sm font-semibold text-slate-900">{formatDate(booking.returnDate)}</p>
                                                </div>
                                            </div>

                                            {/* Countdown */}
                                            <div className="rounded-2xl bg-gradient-to-br from-emerald-50 via-cyan-50 to-blue-50 p-4 mb-4 border border-emerald-100">
                                                <p className="text-xs font-medium text-emerald-700 uppercase tracking-wider">{label}</p>
                                                <p className="text-2xl font-bold bg-gradient-to-r from-emerald-600 via-cyan-600 to-blue-600 bg-clip-text text-transparent">
                                                    {countdown}
                                                </p>
                                            </div>

                                            <div className="flex items-center justify-between">
                                                <p className="text-lg font-bold text-slate-900">€{booking.totalPrice.toFixed(2)}</p>
                                                {canCancel(booking) && (
                                                    <Button
                                                        onClick={() => handleCancel(booking.id)}
                                                        disabled={cancellingId === booking.id}
                                                        variant="destructive"
                                                        size="sm"
                                                        className="rounded-full"
                                                    >
                                                        {cancellingId === booking.id ? "Cancelling..." : "Cancel"}
                                                    </Button>
                                                )}
                                            </div>
                                        </div>
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
                            <div className="rounded-3xl border border-white/15 bg-white/75 p-8 text-center shadow-2xl backdrop-blur">
                                <p className="text-slate-600">
                                    No booking history yet. Complete a rental to see it here!
                                </p>
                            </div>
                        ) : (
                            <div className="space-y-4">
                                {historyBookings.map((booking) => (
                                    <div
                                        key={booking.id}
                                        className="rounded-3xl border border-white/15 bg-white/75 p-6 shadow-2xl backdrop-blur transition-all duration-300 hover:shadow-emerald-500/10"
                                    >
                                        <div className="flex items-start justify-between mb-4">
                                            <div>
                                                <h3 className="text-lg font-bold text-slate-900">{booking.productName}</h3>
                                                <p className="text-sm text-slate-600">
                                                    Owner: {booking.ownerName || 'Unknown'}
                                                </p>
                                            </div>
                                            <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusBadge(booking.status)}`}>
                                                {booking.status}
                                            </span>
                                        </div>
                                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                                            <div>
                                                <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">From</p>
                                                <p className="text-sm font-semibold text-slate-900">{formatDate(booking.bookingDate)}</p>
                                            </div>
                                            <div>
                                                <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">To</p>
                                                <p className="text-sm font-semibold text-slate-900">{formatDate(booking.returnDate)}</p>
                                            </div>
                                            <div>
                                                <p className="text-xs font-medium text-slate-500 uppercase tracking-wider">Total Paid</p>
                                                <p className="text-sm font-bold text-slate-900">€{booking.totalPrice.toFixed(2)}</p>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </>
                )}
                    </div>
                </section>
            </main>
        </div>
    )
}
