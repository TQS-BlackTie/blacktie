import { useEffect, useState } from "react"
import { getUserReputation, type User } from "../lib/api"
import { Star, X, User as UserIcon, Shield, ShoppingBag } from "lucide-react"

type Props = {
    userId: number
    onClose: () => void
}

export default function UserProfileModal({ userId, onClose }: Props) {
    const [profile, setProfile] = useState<User | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState("")

    useEffect(() => {
        getUserReputation(userId)
            .then(setProfile)
            .catch(() => setError("Failed to load profile"))
            .finally(() => setLoading(false))
    }, [userId])

    if (!userId) return null

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
            <div className="bg-white rounded-lg shadow-xl w-full max-w-lg overflow-hidden animate-in fade-in zoom-in-95 duration-200">
                <div className="relative h-32 bg-gray-900">
                    <button
                        onClick={onClose}
                        className="absolute top-4 right-4 p-2 bg-black/20 hover:bg-black/40 text-white rounded-full transition-colors"
                    >
                        <X size={20} />
                    </button>
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
                                <h2 className="text-2xl font-bold text-slate-900">{profile.name}</h2>
                                <div className="text-sm text-slate-500 capitalize">{profile.role}</div>
                                {profile.businessInfo && (
                                    <p className="mt-2 text-sm text-slate-600">{profile.businessInfo}</p>
                                )}
                                <div className="mt-1 text-xs text-slate-500">
                                    Member since {new Date(profile.createdAt).toLocaleDateString()}
                                </div>
                            </div>

                            <div className="grid grid-cols-2 gap-4">
                                {/* Renter Reputation */}
                                <div className="bg-blue-50 p-4 rounded-lg border border-blue-100">
                                    <div className="flex items-center gap-2 mb-2 text-blue-700 font-medium">
                                        <ShoppingBag size={16} />
                                        <span>Renter Rating</span>
                                    </div>
                                    <div className="flex items-baseline gap-2">
                                        <span className="text-3xl font-bold text-blue-900">
                                            {(profile.renterAverageRating || 0).toFixed(1)}
                                        </span>
                                        <div className="flex items-center text-yellow-500">
                                            <Star size={16} fill="currentColor" />
                                        </div>
                                    </div>
                                    <div className="text-xs text-blue-600 mt-1">
                                        {profile.renterReviewCount || 0} reviews
                                    </div>
                                </div>

                                {/* Owner Reputation */}
                                <div className="bg-purple-50 p-4 rounded-lg border border-purple-100">
                                    <div className="flex items-center gap-2 mb-2 text-purple-700 font-medium">
                                        <Shield size={16} />
                                        <span>Owner Rating</span>
                                    </div>
                                    <div className="flex items-baseline gap-2">
                                        <span className="text-3xl font-bold text-purple-900">
                                            {(profile.ownerAverageRating || 0).toFixed(1)}
                                        </span>
                                        <div className="flex items-center text-yellow-500">
                                            <Star size={16} fill="currentColor" />
                                        </div>
                                    </div>
                                    <div className="text-xs text-purple-600 mt-1">
                                        {profile.ownerReviewCount || 0} reviews
                                    </div>
                                </div>
                            </div>

                            <div className="mt-6 pt-6 border-t flex justify-between items-center text-sm text-slate-500">
                                <div>Total Reviews: <span className="font-medium text-slate-900">{profile.totalReviews || 0}</span></div>
                                <div>Overall Rating: <span className="font-medium text-slate-900">{(profile.averageRating || 0).toFixed(1)}</span> / 5.0</div>
                            </div>
                        </>
                    ) : null}
                </div>
            </div>
        </div>
    )
}
