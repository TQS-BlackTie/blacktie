import { useEffect, useState } from 'react'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { requestDeposit } from '@/lib/api'

interface User {
  id: number
  name: string
  email: string
  role: string
}

interface Booking {
  id: number
  renterId: number
  renterName: string
  productId: number
  productName: string
  bookingDate: string
  returnDate: string
  totalPrice: number
  status: string
  depositAmount?: number
  depositRequested?: boolean
  depositReason?: string
  depositRequestedAt?: string
  depositPaid?: boolean
  depositPaidAt?: string
}

export default function OwnerBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [filteredBookings, setFilteredBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [user, setUser] = useState<User | null>(null)
  const [filterType, setFilterType] = useState<'all' | 'year' | 'month' | 'week'>('all')
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear())
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1)
  const [selectedWeek, setSelectedWeek] = useState<number>(1)
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null)
  const [depositModalOpen, setDepositModalOpen] = useState(false)
  const [depositAmount, setDepositAmount] = useState('')
  const [depositReason, setDepositReason] = useState('')
  const [depositError, setDepositError] = useState<string | null>(null)
  const [depositSubmitting, setDepositSubmitting] = useState(false)

  useEffect(() => {
    const userData = window.localStorage.getItem('user')
    if (!userData) {
      window.location.href = '/login'
      return
    }

    try {
      const parsedUser = JSON.parse(userData) as User
      
      if (parsedUser.role !== 'owner') {
        window.location.href = '/'
        return
      }

      setUser(parsedUser)
      fetchBookings(parsedUser.id)
    } catch (error) {
      console.error('Failed to parse user data', error)
      window.location.href = '/login'
    }
  }, [])

  const fetchBookings = async (ownerId: number) => {
    try {
      setLoading(true)
      const response = await fetch('/api/bookings/owner/history', {
        headers: {
          'X-User-Id': ownerId.toString()
        }
      })

      if (!response.ok) {
        throw new Error('Failed to fetch bookings')
      }

      const data = await response.json()
      setBookings(data)
      setFilteredBookings(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    applyFilters()
  }, [filterType, selectedYear, selectedMonth, selectedWeek, bookings])

  const getWeekOfMonth = (date: Date) => {
    const firstDay = new Date(date.getFullYear(), date.getMonth(), 1)
    const dayOfMonth = date.getDate()
    const dayOfWeek = firstDay.getDay()
    return Math.ceil((dayOfMonth + dayOfWeek) / 7)
  }

  const applyFilters = () => {
    let filtered = [...bookings]

    if (filterType === 'year') {
      filtered = filtered.filter(booking => {
        const bookingYear = new Date(booking.bookingDate).getFullYear()
        return bookingYear === selectedYear
      })
    } else if (filterType === 'month') {
      filtered = filtered.filter(booking => {
        const bookingDate = new Date(booking.bookingDate)
        return bookingDate.getFullYear() === selectedYear && 
               bookingDate.getMonth() + 1 === selectedMonth
      })
    } else if (filterType === 'week') {
      filtered = filtered.filter(booking => {
        const bookingDate = new Date(booking.bookingDate)
        return bookingDate.getFullYear() === selectedYear && 
               bookingDate.getMonth() + 1 === selectedMonth &&
               getWeekOfMonth(bookingDate) === selectedWeek
      })
    }

    setFilteredBookings(filtered)
  }

  const getAvailableYears = () => {
    const years = bookings.map(b => new Date(b.bookingDate).getFullYear())
    return [...new Set(years)].sort((a, b) => b - a)
  }

  const getWeeksInMonth = () => {
    const lastDay = new Date(selectedYear, selectedMonth, 0).getDate()
    const firstDayOfWeek = new Date(selectedYear, selectedMonth - 1, 1).getDay()
    return Math.ceil((lastDay + firstDayOfWeek) / 7)
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-PT', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('pt-PT', {
      style: 'currency',
      currency: 'EUR'
    }).format(price)
  }

  const getStatusBadgeColor = (status: string) => {
    switch (status) {
      case 'PENDING_APPROVAL':
        return 'bg-yellow-100 text-yellow-800'
      case 'APPROVED':
        return 'bg-blue-100 text-blue-800'
      case 'REJECTED':
        return 'bg-gray-100 text-gray-800'
      case 'PAID':
        return 'bg-green-100 text-green-800'
      case 'COMPLETED':
        return 'bg-green-600 text-white'
      case 'CANCELLED':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const mapStatusToLabel = (status: string) => {
    switch (status) {
      case 'PENDING_APPROVAL':
        return 'Pending'
      case 'APPROVED':
        return 'Approved'
      case 'REJECTED':
        return 'Rejected'
      case 'PAID':
        return 'Paid'
      case 'COMPLETED':
        return 'Completed'
      case 'CANCELLED':
        return 'Cancelled'
      default:
        return status
    }
  }

  const handleLogout = () => {
    window.localStorage.removeItem('user')
    window.location.href = '/login'
  }

  const canRequestDeposit = (booking: Booking) => {
    const returnDate = new Date(booking.returnDate)
    const now = new Date()
    return (
      (booking.status === 'COMPLETED' || booking.status === 'PAID') &&
      returnDate < now &&
      !booking.depositRequested
    )
  }

  const handleRequestDepositClick = (booking: Booking) => {
    setSelectedBooking(booking)
    setDepositAmount('')
    setDepositReason('')
    setDepositError(null)
    setDepositModalOpen(true)
  }

  const handleRequestDepositSubmit = async () => {
    if (!selectedBooking || !user) return

    const amount = Number(depositAmount)
    if (!depositAmount || isNaN(amount) || amount <= 0) {
      setDepositError('Please enter a valid deposit amount')
      return
    }

    if (!depositReason.trim()) {
      setDepositError('Please provide a reason for the deposit')
      return
    }

    try {
      setDepositSubmitting(true)
      setDepositError(null)
      await requestDeposit(user.id, selectedBooking.id, {
        depositAmount: amount,
        reason: depositReason.trim()
      })
      
      // Refresh bookings
      await fetchBookings(user.id)
      setDepositModalOpen(false)
    } catch (error) {
      setDepositError(error instanceof Error ? error.message : 'Failed to request deposit')
    } finally {
      setDepositSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="text-lg">Loading bookings...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800 text-white">
      <Navbar 
        userName={user?.name}
        userRole={user?.role}
        onLogout={handleLogout}
        notificationBell={user ? <NotificationBell userId={user.id} /> : undefined}
      />
      
      <div className="container mx-auto p-6">
        <div className="mb-6">
          <h1 className="text-3xl font-bold">Booking History</h1>
          <p className="text-muted-foreground mt-2">
            All bookings for your products
          </p>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-destructive/10 border border-destructive/20 rounded-lg text-destructive">
            {error}
          </div>
        )}

        {/* Filtros */}
        <div className="mb-6 bg-card p-4 rounded-lg border">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Filter by
              </label>
              <select
                value={filterType}
                onChange={(e) => setFilterType(e.target.value as 'all' | 'year' | 'month' | 'week')}
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary focus:border-primary bg-white text-gray-900"
              >
                <option value="all">All Bookings</option>
                <option value="year">By Year</option>
                <option value="month">By Month</option>
                <option value="week">By Week</option>
              </select>
            </div>

            {(filterType === 'year' || filterType === 'month' || filterType === 'week') && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Year
                </label>
                <select
                  value={selectedYear}
                  onChange={(e) => setSelectedYear(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary focus:border-primary bg-white text-gray-900"
                >
                  {getAvailableYears().length > 0 ? (
                    getAvailableYears().map(year => (
                      <option key={year} value={year}>{year}</option>
                    ))
                  ) : (
                    <option value={new Date().getFullYear()}>{new Date().getFullYear()}</option>
                  )}
                </select>
              </div>
            )}

            {(filterType === 'month' || filterType === 'week') && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Month
                </label>
                <select
                  value={selectedMonth}
                  onChange={(e) => setSelectedMonth(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary focus:border-primary bg-white text-gray-900"
                >
                  <option value={1}>January</option>
                  <option value={2}>February</option>
                  <option value={3}>March</option>
                  <option value={4}>April</option>
                  <option value={5}>May</option>
                  <option value={6}>June</option>
                  <option value={7}>July</option>
                  <option value={8}>August</option>
                  <option value={9}>September</option>
                  <option value={10}>October</option>
                  <option value={11}>November</option>
                  <option value={12}>December</option>
                </select>
              </div>
            )}

            {filterType === 'week' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Week
                </label>
                <select
                  value={selectedWeek}
                  onChange={(e) => setSelectedWeek(Number(e.target.value))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary focus:border-primary bg-white text-gray-900"
                >
                  {Array.from({ length: getWeeksInMonth() }, (_, i) => i + 1).map(week => (
                    <option key={week} value={week}>Week {week}</option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <div className="mt-3 text-sm text-gray-600">
            Showing {filteredBookings.length} of {bookings.length} bookings
          </div>
        </div>

        {filteredBookings.length === 0 ? (
          <div className="bg-card p-8 rounded-lg border text-center">
            <p className="text-muted-foreground">
              {bookings.length === 0 ? 'No bookings yet' : 'No bookings found for the selected filters'}
            </p>
          </div>
        ) : (
          <div className="bg-card rounded-lg border overflow-hidden">
            <table className="min-w-full divide-y divide-border">
              <thead className="bg-muted/50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Product
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Customer
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Start Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    End Date
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Amount
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-card divide-y divide-border">
                {filteredBookings.map((booking) => (
                  <tr key={booking.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {booking.productName}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-900">{booking.renterName}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-500">
                        {formatDate(booking.bookingDate)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm text-gray-500">
                        {formatDate(booking.returnDate)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">
                        {formatPrice(booking.totalPrice)}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusBadgeColor(booking.status)}`}>
                        {mapStatusToLabel(booking.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm">
                      {canRequestDeposit(booking) ? (
                        <Button
                          onClick={() => handleRequestDepositClick(booking)}
                          size="sm"
                          variant="outline"
                          className="text-xs"
                        >
                          Request Deposit
                        </Button>
                      ) : booking.depositRequested ? (
                        <div className="text-xs text-muted-foreground">
                          {booking.depositPaid ? (
                            <span className="text-green-600 font-medium">Deposit Paid</span>
                          ) : (
                            <span className="text-amber-600 font-medium">Deposit Pending</span>
                          )}
                        </div>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {filteredBookings.length > 0 && (
          <div className="mt-6 bg-card p-6 rounded-lg border">
            <h2 className="text-lg text-primary font-semibold mb-4">Summary</h2>
            <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-yellow-600">
                  {filteredBookings.filter(b => b.status === 'PENDING_APPROVAL').length}
                </div>
                <div className="text-sm text-muted-foreground">Pending</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-600">
                  {filteredBookings.filter(b => b.status === 'PAID' || b.status === 'APPROVED').length}
                </div>
                <div className="text-sm text-muted-foreground">Active</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-700">
                  {filteredBookings.filter(b => b.status === 'COMPLETED').length}
                </div>
                <div className="text-sm text-muted-foreground">Completed</div>
              </div>
              <div className="text-center">
                <div className="text-2xl text-green-950 font-bold">
                  {formatPrice(filteredBookings.filter(b => b.status === 'PAID' || b.status === 'COMPLETED').reduce((sum, b) => sum + b.totalPrice, 0))}
                </div>
                <div className="text-sm text-muted-foreground">Total Revenue</div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Deposit Request Modal */}
      <Dialog open={depositModalOpen} onOpenChange={setDepositModalOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Request Deposit</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {selectedBooking && (
              <div className="text-sm text-muted-foreground mb-4">
                <p><strong>Product:</strong> {selectedBooking.productName}</p>
                <p><strong>Renter:</strong> {selectedBooking.renterName}</p>
              </div>
            )}
            
            {depositError && (
              <div className="p-3 bg-red-50 border border-red-200 rounded text-red-600 text-sm">
                {depositError}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium mb-2">
                Deposit Amount (€)
              </label>
              <Input
                type="number"
                min="0"
                step="0.01"
                placeholder="Enter amount"
                value={depositAmount}
                onChange={(e) => setDepositAmount(e.target.value)}
                disabled={depositSubmitting}
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">
                Reason for Deposit
              </label>
              <textarea
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-primary focus:border-primary"
                rows={4}
                placeholder="Explain why you're requesting this deposit..."
                value={depositReason}
                onChange={(e) => setDepositReason(e.target.value)}
                disabled={depositSubmitting}
              />
            </div>

            <div className="flex gap-3 justify-end">
              <Button
                variant="outline"
                onClick={() => setDepositModalOpen(false)}
                disabled={depositSubmitting}
                className="border-gray-300 text-gray-700 hover:bg-gray-50"
              >
                Cancel
              </Button>
              <Button
                onClick={handleRequestDepositSubmit}
                disabled={depositSubmitting}
                className="bg-blue-600 hover:bg-blue-700 text-white"
              >
                {depositSubmitting ? 'Requesting...' : 'Request Deposit'}
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
