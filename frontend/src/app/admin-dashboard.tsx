import { useEffect, useState } from 'react'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'
import { Button } from '@/components/ui/button'
import {
  getAdminMetrics,
  getAdminUsers,
  updateUserStatus,
  updateUserRoleAdmin,
  deleteUserAdmin,
  getAdminProducts,
  deleteProductAdmin,
  type PlatformMetrics,
  type AdminUser,
  type AdminProduct
} from '@/lib/api'

interface User {
  id: number
  name: string
  email: string
  role: string
}

export default function AdminDashboardPage() {
  const [user, setUser] = useState<User | null>(null)
  const [metrics, setMetrics] = useState<PlatformMetrics | null>(null)
  const [users, setUsers] = useState<AdminUser[]>([])
  const [products, setProducts] = useState<AdminProduct[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<'overview' | 'users' | 'products'>('overview')
  const [actionLoading, setActionLoading] = useState<number | null>(null)
  const [productActionLoading, setProductActionLoading] = useState<number | null>(null)

  useEffect(() => {
    const userData = localStorage.getItem('user')
    if (!userData) {
      window.location.href = '/login'
      return
    }

    try {
      const parsedUser = JSON.parse(userData) as User
      if (parsedUser.role !== 'admin') {
        window.location.href = '/'
        return
      }
      setUser(parsedUser)
      loadData(parsedUser.id)
    } catch {
      window.location.href = '/login'
    }
  }, [])

  const loadData = async (userId: number) => {
    try {
      setLoading(true)
      const [metricsData, usersData, productsData] = await Promise.all([
        getAdminMetrics(userId),
        getAdminUsers(userId),
        getAdminProducts(userId)
      ])
      setMetrics(metricsData)
      setUsers(usersData)
      setProducts(productsData)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('user')
    window.location.href = '/login'
  }

  const handleStatusChange = async (targetUserId: number, newStatus: string) => {
    if (!user) return
    try {
      setActionLoading(targetUserId)
      const updatedUser = await updateUserStatus(user.id, targetUserId, newStatus)
      setUsers(prev => prev.map(u => u.id === targetUserId ? updatedUser : u))
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update status')
    } finally {
      setActionLoading(null)
    }
  }

  const handleRoleChange = async (targetUserId: number, newRole: string) => {
    if (!user) return
    try {
      setActionLoading(targetUserId)
      const updatedUser = await updateUserRoleAdmin(user.id, targetUserId, newRole)
      setUsers(prev => prev.map(u => u.id === targetUserId ? updatedUser : u))
      // Refresh metrics since role counts changed
      const newMetrics = await getAdminMetrics(user.id)
      setMetrics(newMetrics)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to update role')
    } finally {
      setActionLoading(null)
    }
  }

  const handleDeleteUser = async (targetUserId: number) => {
    if (!user) return
    if (!confirm('Are you sure you want to delete this user? This action cannot be undone.')) return
    
    try {
      setActionLoading(targetUserId)
      await deleteUserAdmin(user.id, targetUserId)
      setUsers(prev => prev.filter(u => u.id !== targetUserId))
      // Refresh metrics and products
      const [newMetrics, newProducts] = await Promise.all([
        getAdminMetrics(user.id),
        getAdminProducts(user.id)
      ])
      setMetrics(newMetrics)
      setProducts(newProducts)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete user')
    } finally {
      setActionLoading(null)
    }
  }

  const handleDeleteProduct = async (productId: number, productName: string) => {
    if (!user) return
    if (!confirm(`Are you sure you want to delete "${productName}"? All active bookings will be cancelled and affected users will be notified.`)) return
    
    try {
      setProductActionLoading(productId)
      await deleteProductAdmin(user.id, productId)
      setProducts(prev => prev.filter(p => p.id !== productId))
      // Refresh metrics
      const newMetrics = await getAdminMetrics(user.id)
      setMetrics(newMetrics)
    } catch (err) {
      alert(err instanceof Error ? err.message : 'Failed to delete product')
    } finally {
      setProductActionLoading(null)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gradient-to-b from-slate-900 to-slate-800 text-white">
        <div className="text-lg text-slate-600">Loading admin dashboard...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-gradient-to-b from-slate-900 to-slate-800 text-white gap-4">
        <div className="text-lg text-red-600">{error}</div>
        <Button onClick={() => user && loadData(user.id)}>Retry</Button>
      </div>
    )
  }

  if (!user || !metrics) return null

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800 text-white">
      <Navbar
        userName={user.name}
        userRole={user.role}
        onLogout={handleLogout}
        notificationBell={<NotificationBell userId={user.id} />}
      />

      <main className="relative z-10">
        <section className="w-full px-6 pb-12 mt-8 md:px-12 lg:px-20">
          <div className="rounded-3xl border border-white/15 bg-white/75 p-4 text-slate-900 shadow-2xl backdrop-blur md:p-8">
            <h1 className="text-3xl font-bold mb-6 text-slate-900">Admin Dashboard</h1>

            {/* Tab Navigation */}
            <div className="flex gap-2 mb-8 border-b border-slate-200 pb-4">
              <button
                onClick={() => setActiveTab('overview')}
                className={`px-6 py-2 rounded-lg font-medium transition ${
                  activeTab === 'overview'
                    ? 'bg-emerald-500 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                Platform Overview
              </button>
              <button
                onClick={() => setActiveTab('users')}
                className={`px-6 py-2 rounded-lg font-medium transition ${
                  activeTab === 'users'
                    ? 'bg-emerald-500 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                User Management
              </button>
              <button
                onClick={() => setActiveTab('products')}
                className={`px-6 py-2 rounded-lg font-medium transition ${
                  activeTab === 'products'
                    ? 'bg-emerald-500 text-white'
                    : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                }`}
              >
                Product Catalog
              </button>
            </div>

            {activeTab === 'overview' && (
              <div className="space-y-8">
                {/* KPI Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                  <MetricCard
                    title="Total Users"
                    value={metrics.totalUsers}
                    subtitle={`${metrics.totalOwners} owners, ${metrics.totalRenters} renters`}
                    color="blue"
                  />
                  <MetricCard
                    title="Total Products"
                    value={metrics.totalProducts}
                    subtitle={`${metrics.availableProducts} available`}
                    color="green"
                  />
                  <MetricCard
                    title="Total Bookings"
                    value={metrics.totalBookings}
                    subtitle={`${metrics.activeBookings} active`}
                    color="purple"
                  />
                  <MetricCard
                    title="Total Revenue"
                    value={`€${metrics.totalRevenue.toFixed(2)}`}
                    subtitle={`Avg: €${metrics.averageBookingValue.toFixed(2)}/booking`}
                    color="emerald"
                  />
                </div>

                {/* Detailed Stats */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {/* User Distribution */}
                  <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-100">
                    <h3 className="text-lg font-semibold mb-4 text-slate-800">User Distribution</h3>
                    <div className="space-y-3">
                      <StatBar label="Owners" value={metrics.totalOwners} total={metrics.totalUsers} color="emerald" />
                      <StatBar label="Renters" value={metrics.totalRenters} total={metrics.totalUsers} color="blue" />
                    </div>
                  </div>

                  {/* Booking Status */}
                  <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-100">
                    <h3 className="text-lg font-semibold mb-4 text-slate-800">Booking Status</h3>
                    <div className="space-y-3">
                      <StatBar label="Active" value={metrics.activeBookings} total={metrics.totalBookings} color="green" />
                      <StatBar label="Completed" value={metrics.completedBookings} total={metrics.totalBookings} color="blue" />
                      <StatBar label="Cancelled" value={metrics.cancelledBookings} total={metrics.totalBookings} color="red" />
                    </div>
                  </div>
                </div>

                {/* Compliance & Safety Section */}
                <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-100">
                  <h3 className="text-lg font-semibold mb-4 text-slate-800">Platform Health & Compliance</h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <div className="p-4 bg-green-50 rounded-lg border border-green-100">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-green-500"></div>
                        <span className="font-medium text-green-800">System Status</span>
                      </div>
                      <p className="text-sm text-green-600 mt-1">All systems operational</p>
                    </div>
                    <div className="p-4 bg-blue-50 rounded-lg border border-blue-100">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                        <span className="font-medium text-blue-800">Product Availability</span>
                      </div>
                      <p className="text-sm text-blue-600 mt-1">
                        {((metrics.availableProducts / Math.max(metrics.totalProducts, 1)) * 100).toFixed(0)}% products available
                      </p>
                    </div>
                    <div className="p-4 bg-purple-50 rounded-lg border border-purple-100">
                      <div className="flex items-center gap-2">
                        <div className="w-3 h-3 rounded-full bg-purple-500"></div>
                        <span className="font-medium text-purple-800">Booking Success Rate</span>
                      </div>
                      <p className="text-sm text-purple-600 mt-1">
                        {metrics.totalBookings > 0 
                          ? ((metrics.completedBookings / metrics.totalBookings) * 100).toFixed(0)
                          : 0}% completion rate
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'users' && (
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <h2 className="text-xl font-semibold text-slate-800">
                    All Users ({users.length})
                  </h2>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-slate-200">
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">User</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Role</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Status</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Activity</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Joined</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map((u) => (
                        <tr key={u.id} className="border-b border-slate-100 hover:bg-slate-50">
                          <td className="py-4 px-4">
                            <div>
                              <div className="font-medium text-slate-900">{u.name}</div>
                              <div className="text-sm text-slate-500">{u.email}</div>
                            </div>
                          </td>
                          <td className="py-4 px-4">
                            <select
                              value={u.role}
                              onChange={(e) => handleRoleChange(u.id, e.target.value)}
                              disabled={actionLoading === u.id}
                              className="px-2 py-1 rounded border border-slate-200 text-sm bg-white"
                            >
                              <option value="renter">Renter</option>
                              <option value="owner">Owner</option>
                            </select>
                          </td>
                          <td className="py-4 px-4">
                            <select
                              value={u.status}
                              onChange={(e) => handleStatusChange(u.id, e.target.value)}
                              disabled={actionLoading === u.id}
                              className={`px-2 py-1 rounded border text-sm ${
                                u.status === 'active' ? 'bg-green-50 border-green-200 text-green-700' :
                                u.status === 'suspended' ? 'bg-yellow-50 border-yellow-200 text-yellow-700' :
                                'bg-red-50 border-red-200 text-red-700'
                              }`}
                            >
                              <option value="active">Active</option>
                              <option value="suspended">Suspended</option>
                              <option value="banned">Banned</option>
                            </select>
                          </td>
                          <td className="py-4 px-4">
                            <div className="text-sm">
                              <div className="text-slate-600">{u.bookingsCount} bookings</div>
                              <div className="text-slate-600">{u.productsCount} products</div>
                            </div>
                          </td>
                          <td className="py-4 px-4 text-sm text-slate-500">
                            {new Date(u.createdAt).toLocaleDateString()}
                          </td>
                          <td className="py-4 px-4">
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleDeleteUser(u.id)}
                              disabled={actionLoading === u.id}
                              className="bg-red-500 hover:bg-red-600 text-white text-xs"
                            >
                              {actionLoading === u.id ? '...' : 'Delete'}
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {users.length === 0 && (
                    <div className="text-center py-12 text-slate-500">
                      No users found
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'products' && (
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <h2 className="text-xl font-semibold text-slate-800">
                    All Products ({products.length})
                  </h2>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b border-slate-200">
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Product</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Owner</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Price</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Status</th>
                        <th className="text-left py-3 px-4 font-semibold text-slate-700">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {products.map((product) => (
                        <tr key={product.id} className="border-b border-slate-100 hover:bg-slate-50">
                          <td className="py-4 px-4">
                            <div>
                              <div className="font-medium text-slate-900">{product.name}</div>
                              <div className="text-sm text-slate-500 max-w-md truncate">
                                {product.description}
                              </div>
                            </div>
                          </td>
                          <td className="py-4 px-4">
                            <div>
                              <div className="font-medium text-slate-700">
                                {product.owner?.name || 'N/A'}
                              </div>
                              <div className="text-sm text-slate-500">
                                {product.owner?.email || 'N/A'}
                              </div>
                            </div>
                          </td>
                          <td className="py-4 px-4">
                            <span className="font-medium text-slate-900">
                              €{product.price.toFixed(2)}
                            </span>
                            <span className="text-sm text-slate-500">/day</span>
                          </td>
                          <td className="py-4 px-4">
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                              product.available 
                                ? 'bg-green-100 text-green-700' 
                                : 'bg-red-100 text-red-700'
                            }`}>
                              {product.available ? 'Available' : 'Unavailable'}
                            </span>
                          </td>
                          <td className="py-4 px-4">
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleDeleteProduct(product.id, product.name)}
                              disabled={productActionLoading === product.id}
                              className="bg-red-500 hover:bg-red-600 text-white text-xs"
                            >
                              {productActionLoading === product.id ? '...' : 'Delete'}
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {products.length === 0 && (
                    <div className="text-center py-12 text-slate-500">
                      No products found
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  )
}

function MetricCard({ 
  title, 
  value, 
  subtitle, 
  color 
}: { 
  title: string
  value: string | number
  subtitle: string
  color: 'blue' | 'green' | 'purple' | 'emerald' 
}) {
  const colors = {
    blue: 'bg-blue-50 border-blue-100',
    green: 'bg-green-50 border-green-100',
    purple: 'bg-purple-50 border-purple-100',
    emerald: 'bg-emerald-50 border-emerald-100'
  }

  const textColors = {
    blue: 'text-blue-900',
    green: 'text-green-900',
    purple: 'text-purple-900',
    emerald: 'text-emerald-900'
  }

  return (
    <div className={`p-6 rounded-xl border ${colors[color]}`}>
      <h3 className="text-sm font-medium text-slate-600">{title}</h3>
      <p className={`text-3xl font-bold mt-2 ${textColors[color]}`}>{value}</p>
      <p className="text-sm text-slate-500 mt-1">{subtitle}</p>
    </div>
  )
}

function StatBar({ 
  label, 
  value, 
  total, 
  color 
}: { 
  label: string
  value: number
  total: number
  color: 'emerald' | 'blue' | 'green' | 'red' 
}) {
  const percentage = total > 0 ? (value / total) * 100 : 0
  
  const colors = {
    emerald: 'bg-emerald-500',
    blue: 'bg-blue-500',
    green: 'bg-green-500',
    red: 'bg-red-500'
  }

  return (
    <div>
      <div className="flex justify-between text-sm mb-1">
        <span className="text-slate-600">{label}</span>
        <span className="font-medium text-slate-800">{value}</span>
      </div>
      <div className="h-2 bg-slate-100 rounded-full overflow-hidden">
        <div
          className={`h-full ${colors[color]} rounded-full transition-all`}
          style={{ width: `${percentage}%` }}
        />
      </div>
    </div>
  )
}
