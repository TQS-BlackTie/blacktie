import { useEffect, useState } from "react"
import { getUserReputation, type User } from "../lib/api"
import { Star, User as UserIcon, Shield, ShoppingBag } from "lucide-react"
import { Navbar } from "../components/navbar"

export default function MyReputationPage() {
    const [profile, setProfile] = useState<User | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState("")

    // Get user ID from local storage (simplified auth)
    const userId = typeof window !== "undefined" ? Number(localStorage.getItem("userId")) : 0
    const userName = typeof window !== "undefined" ? localStorage.getItem("userName") || "" : ""
    const userRole = typeof window !== "undefined" ? localStorage.getItem("userRole") || "" : ""

    const handleLogout = () => {
        localStorage.clear()
        window.location.href = "/login"
    }

    useEffect(() => {
        if (!userId) {
            window.location.href = "/login"
            return
        }

        getUserReputation(userId)
            .then(setProfile)
            .catch(() => setError("Failed to load profile"))
            .finally(() => setLoading(false))
    }, [userId])

    return (
        <div className="min-h-screen bg-slate-950 text-slate-200">
            <Navbar userName={userName} userRole={userRole} onLogout={handleLogout} />

            <main className="container mx-auto px-4 py-8">
                <h1 className="text-3xl font-bold mb-8 bg-gradient-to-r from-emerald-400 to-cyan-400 bg-clip-text text-transparent">
                    My Reputation
                </h1>

                <div className="bg-white text-gray-900 rounded-lg shadow-xl w-full max-w-2xl mx-auto overflow-hidden">
                    <div className="relative h-32 bg-gray-900">
                    </div>

                    <div className="px-6 pb-6">
                        <div className="relative -mt-12 mb-4 flex justify-between items-end">
                            <div className="w-24 h-24 bg-white rounded-full p-1 shadow-lg">
                                <div className="w-full h-full bg-gray-100 rounded-full flex items-center justify-center text-gray-400">
                                    <UserIcon size={40} />
                                </div>
                            </div>
                        </div>

                        {loading ? (
                            <div className="space-y-4 animate-pulse">
                                <div className="h-6 w-1/3 bg-gray-200 rounded"></div>
                                <div className="h-4 w-1/4 bg-gray-200 rounded"></div>
                                <div className="h-20 bg-gray-100 rounded"></div>
                            </div>
                        ) : error ? (
                            <div className="text-center py-8 text-red-500">{error}</div>
                        ) : profile ? (
                            <>
                                <div className="mb-6">
                                    <h2 className="text-2xl font-bold text-gray-900">{profile.name}</h2>
                                    <div className="text-sm text-gray-500 capitalize">{profile.role}</div>
                                    {profile.businessInfo && (
                                        <p className="mt-2 text-sm text-gray-600">{profile.businessInfo}</p>
                                    )}
                                    <div className="mt-1 text-xs text-gray-400">
                                        Member since {new Date(profile.createdAt).toLocaleDateString()}
                                    </div>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    {/* Renter Reputation */}
                                    <div className="bg-blue-50 p-6 rounded-lg border border-blue-100">
                                        <div className="flex items-center gap-2 mb-2 text-blue-700 font-medium">
                                            <ShoppingBag size={20} />
                                            <span>Renter Rating</span>
                                        </div>
                                        <div className="flex items-baseline gap-2">
                                            <span className="text-4xl font-bold text-blue-900">
                                                {(profile.renterAverageRating || 0).toFixed(1)}
                                            </span>
                                            <div className="flex items-center text-yellow-500">
                                                <Star size={20} fill="currentColor" />
                                            </div>
                                        </div>
                                        <div className="text-sm text-blue-600 mt-1">
                                            Based on {profile.renterReviewCount || 0} reviews
                                        </div>
                                    </div>

                                    {/* Owner Reputation */}
                                    <div className="bg-purple-50 p-6 rounded-lg border border-purple-100">
                                        <div className="flex items-center gap-2 mb-2 text-purple-700 font-medium">
                                            <Shield size={20} />
                                            <span>Owner Rating</span>
                                        </div>
                                        <div className="flex items-baseline gap-2">
                                            <span className="text-4xl font-bold text-purple-900">
                                                {(profile.ownerAverageRating || 0).toFixed(1)}
                                            </span>
                                            <div className="flex items-center text-yellow-500">
                                                <Star size={20} fill="currentColor" />
                                            </div>
                                        </div>
                                        <div className="text-sm text-purple-600 mt-1">
                                            Based on {profile.ownerReviewCount || 0} reviews
                                        </div>
                                    </div>
                                </div>

                                <div className="mt-8 pt-6 border-t flex justify-between items-center text-gray-500">
                                    <div>Total Reviews Received: <span className="font-medium text-gray-900">{profile.totalReviews || 0}</span></div>
                                    <div>Overall Rating: <span className="font-medium text-gray-900">{(profile.averageRating || 0).toFixed(1)}</span> / 5.0</div>
                                </div>
                            </>
                        ) : null}
                    </div>
                </div>
            </main>
        </div>
    )
}
