import { useEffect, useState } from 'react'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'

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
}

export default function OwnerBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [user, setUser] = useState<User | null>(null)

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
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred')
    } finally {
      setLoading(false)
    }
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
        return 'Pendente'
      case 'APPROVED':
        return 'Aprovada'
      case 'REJECTED':
        return 'Rejeitada'
      case 'PAID':
        return 'Paga'
      case 'COMPLETED':
        return 'Concluída'
      case 'CANCELLED':
        return 'Cancelada'
      default:
        return status
    }
  }

  const handleLogout = () => {
    window.localStorage.removeItem('user')
    window.location.href = '/login'
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="text-lg">A carregar reservas...</div>
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
          <h1 className="text-3xl font-bold">Histórico de Reservas</h1>
          <p className="text-muted-foreground mt-2">
            Todas as reservas dos seus produtos
          </p>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-destructive/10 border border-destructive/20 rounded-lg text-destructive">
            {error}
          </div>
        )}

        {bookings.length === 0 ? (
          <div className="bg-card p-8 rounded-lg border text-center">
            <p className="text-muted-foreground">Não existem reservas ainda</p>
          </div>
        ) : (
          <div className="bg-card rounded-lg border overflow-hidden">
            <table className="min-w-full divide-y divide-border">
              <thead className="bg-muted/50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Produto
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Cliente
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Data Início
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Data Fim
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Valor
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Estado
                  </th>
                </tr>
              </thead>
              <tbody className="bg-card divide-y divide-border">
                {bookings.map((booking) => (
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
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {bookings.length > 0 && (
          <div className="mt-6 bg-card p-6 rounded-lg border">
            <h2 className="text-lg text-primary font-semibold mb-4">Resumo</h2>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-yellow-600">
                  {bookings.filter(b => b.status === 'PENDING_APPROVAL').length}
                </div>
                <div className="text-sm text-muted-foreground">Pendentes</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-600">
                  {bookings.filter(b => b.status === 'PAID').length}
                </div>
                <div className="text-sm text-muted-foreground">Pagas</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-green-700">
                  {bookings.filter(b => b.status === 'COMPLETED').length}
                </div>
                <div className="text-sm text-muted-foreground">Concluídas</div>
              </div>
              <div className="text-center">
                <div className="text-2xl text-green-950 font-bold">
                  {formatPrice(bookings.filter(b => b.status === 'PAID' || b.status === 'COMPLETED').reduce((sum, b) => sum + b.totalPrice, 0))}
                </div>
                <div className="text-sm text-muted-foreground">Total Faturado</div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
