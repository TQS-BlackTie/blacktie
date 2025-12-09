import { useEffect, useState } from 'react'
import { ProductCatalog } from '@/components/product-catalog'

interface User {
  id: number
  name: string
  email: string
  role: string
}

export default function HomePage() {
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

  useEffect(() => {
    if (!user) {
      window.location.href = '/login'
    }
  }, [user])

  const handleLogout = () => {
    localStorage.removeItem('user')
    window.location.href = '/login'
  }

  if (!user) {
    return <div className="flex min-h-screen items-center justify-center">Loading...</div>
  }

  return (
    <div className="min-h-screen p-6 bg-slate-50">
      <header className="flex items-center justify-between max-w-5xl mx-auto mb-6">
        <div>
          <h1 className="text-2xl font-bold">BlackTie Catalog</h1>
          <p className="text-sm text-muted-foreground">
            Welcome, {user.name} ({user.role})
          </p>
        </div>
        <div className="flex gap-3">
          {user.role === 'renter' && (
            <button
              onClick={() => window.location.href = '/history'}
              className="rounded-md bg-blue-500 px-4 py-2 text-white hover:bg-blue-600 text-sm"
            >
              My History
            </button>
          )}
          <button
            onClick={() => window.location.href = '/profile'}
            className="rounded-md bg-slate-200 px-4 py-2 text-slate-900 hover:bg-slate-300 text-sm"
          >
            Manage Profile
          </button>
          <button
            onClick={handleLogout}
            className="rounded-md bg-primary px-4 py-2 text-primary-foreground hover:bg-primary/90 text-sm"
          >
            Logout
          </button>
        </div>
      </header>

      <ProductCatalog userRole={user.role} userId={user.id} />
    </div>
  )
}