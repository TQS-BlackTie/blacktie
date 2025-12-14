import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { Button } from './ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from './ui/dropdown-menu'

interface Notification {
  id: number
  type: string
  message: string
  bookingId: number | null
  isRead: boolean
  createdAt: string
}

interface NotificationBellProps {
  userId: number
}

// Get user role from localStorage
function getUserRole(): string | null {
  if (typeof window === 'undefined') return null
  try {
    const userData = window.localStorage.getItem('user')
    if (!userData) return null
    const user = JSON.parse(userData)
    return user.role || null
  } catch {
    return null
  }
}

// Determine the navigation URL based on notification type
function getNotificationLink(notification: Notification): string | null {
  const type = notification.type
  const userRole = getUserRole()

  // NEW_BOOKING should go to pending approvals page for owners
  if (type === 'NEW_BOOKING') {
    return '/pending-approvals'
  }

  // Notification types that relate to bookings for owners (history/management)
  const ownerBookingTypes = [
    'BOOKING_CANCELLED_BY_RENTER',
    'PAYMENT_RECEIVED',
    'DEPOSIT_PAID',
  ]

  // Notification types that relate to bookings for renters
  const renterBookingTypes = [
    'BOOKING_APPROVED',
    'BOOKING_REJECTED',
    'BOOKING_CANCELLED_BY_OWNER',
    'BOOKING_CANCELLED_BY_ADMIN',
    'DEPOSIT_REQUESTED',
    'DEPOSIT_REFUNDED',
  ]

  // Account-related notifications - no specific page
  const accountTypes = [
    'ACCOUNT_SUSPENDED',
    'ACCOUNT_BANNED',
    'ACCOUNT_REACTIVATED',
  ]

  // Product deleted - go to home/catalog
  if (type === 'PRODUCT_DELETED_BY_ADMIN') {
    return '/'
  }

  // Account notifications - no navigation
  if (accountTypes.includes(type)) {
    return null
  }

  // For booking-related notifications
  if (notification.bookingId) {
    if (ownerBookingTypes.includes(type)) {
      return '/owner-bookings'
    }
    if (renterBookingTypes.includes(type)) {
      return '/my-bookings'
    }
    // Fallback based on user role for unknown booking notification types
    if (userRole === 'owner') {
      return '/owner-bookings'
    }
    return '/my-bookings'
  }

  return null
}

export function NotificationBell({ userId }: NotificationBellProps) {
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [isOpen, setIsOpen] = useState(false)
  const navigate = useNavigate()

  const fetchNotifications = useCallback(async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/notifications?userId=${userId}`)
      if (response.ok) {
        const data = await response.json()
        setNotifications(data)
      }
    } catch (error) {
      console.error('Failed to fetch notifications:', error)
    }
  }, [userId])

  const fetchUnreadCount = useCallback(async () => {
    try {
      const response = await fetch(`http://localhost:8080/api/notifications/unread/count?userId=${userId}`)
      if (response.ok) {
        const data = await response.json()
        setUnreadCount(data.count)
      }
    } catch (error) {
      console.error('Failed to fetch unread count:', error)
    }
  }, [userId])

  const handleNotificationClick = async (notification: Notification) => {
    // Mark as read if not already read
    if (!notification.isRead) {
      try {
        const response = await fetch(
          `http://localhost:8080/api/notifications/${notification.id}/read?userId=${userId}`,
          { method: 'PUT' }
        )
        if (response.ok) {
          fetchNotifications()
          fetchUnreadCount()
        }
      } catch (error) {
        console.error('Failed to mark notification as read:', error)
      }
    }

    // Close the dropdown
    setIsOpen(false)

    // Navigate to the relevant page
    const link = getNotificationLink(notification)
    if (link) {
      navigate(link)
    }
  }

  const markAllAsRead = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/notifications/read-all?userId=${userId}`,
        { method: 'PUT' }
      )
      if (response.ok) {
        fetchNotifications()
        fetchUnreadCount()
      }
    } catch (error) {
      console.error('Failed to mark all as read:', error)
    }
  }

  useEffect(() => {
    const loadUnreadCount = async () => {
      try {
        const response = await fetch(`http://localhost:8080/api/notifications/unread/count?userId=${userId}`)
        if (response.ok) {
          const data = await response.json()
          setUnreadCount(data.count)
        }
      } catch (error) {
        console.error('Failed to fetch unread count:', error)
      }
    }

    loadUnreadCount()
    // Poll for new notifications every 30 seconds
    const interval = setInterval(() => {
      loadUnreadCount()
    }, 30000)

    return () => clearInterval(interval)
  }, [userId])

  useEffect(() => {
    const loadNotifications = async () => {
      try {
        const response = await fetch(`http://localhost:8080/api/notifications?userId=${userId}`)
        if (response.ok) {
          const data = await response.json()
          setNotifications(data)
        }
      } catch (error) {
        console.error('Failed to fetch notifications:', error)
      }
    }

    if (isOpen) {
      loadNotifications()
    }
  }, [isOpen, userId])

  const formatTime = (timestamp: string) => {
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return 'Just now'
    if (diffMins < 60) return `${diffMins}m ago`
    if (diffHours < 24) return `${diffHours}h ago`
    if (diffDays < 7) return `${diffDays}d ago`
    return date.toLocaleDateString()
  }

  return (
    <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="relative">
          <Bell className="h-5 w-5" />
          {unreadCount > 0 && (
            <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center">
              {unreadCount > 9 ? '9+' : unreadCount}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80 max-h-96 overflow-y-auto">
        <div className="flex items-center justify-between px-3 py-2 border-b">
          <h3 className="font-semibold">Notifications</h3>
          {unreadCount > 0 && (
            <button
              onClick={markAllAsRead}
              className="text-xs text-blue-600 hover:text-blue-700"
            >
              Mark all as read
            </button>
          )}
        </div>
        {notifications.length === 0 ? (
          <div className="px-3 py-4 text-sm text-gray-500 text-center">
            No notifications
          </div>
        ) : (
          notifications.map((notification) => {
            const hasLink = getNotificationLink(notification) !== null
            return (
              <DropdownMenuItem
                key={notification.id}
                className={`flex flex-col items-start px-3 py-3 cursor-pointer hover:bg-gray-100 transition-colors ${!notification.isRead ? 'bg-blue-50' : ''
                  } ${hasLink ? 'hover:bg-blue-100' : ''}`}
                onClick={() => handleNotificationClick(notification)}
              >
                <div className="flex items-start justify-between w-full gap-2">
                  <p className="text-sm flex-1">{notification.message}</p>
                  <div className="flex items-center gap-1 flex-shrink-0">
                    {!notification.isRead && (
                      <div className="h-2 w-2 rounded-full bg-blue-500 mt-1" />
                    )}
                    {hasLink && (
                      <svg
                        className="w-4 h-4 text-gray-400 mt-0.5"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M9 5l7 7-7 7"
                        />
                      </svg>
                    )}
                  </div>
                </div>
                <span className="text-xs text-gray-500 mt-1">
                  {formatTime(notification.createdAt)}
                </span>
              </DropdownMenuItem>
            )
          }))
        }
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
