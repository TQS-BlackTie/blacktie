import { useEffect, useState } from 'react'
import { ProductCatalog } from '@/components/product-catalog'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'

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
    <div className="relative min-h-screen overflow-hidden text-slate-50">
      <div className="aurora-blob -left-10 top-8 h-64 w-64 bg-emerald-500/40" />
      <div className="aurora-blob delay-1 right-0 top-40 h-64 w-64 bg-cyan-400/30" />
      <div className="aurora-blob delay-2 -bottom-10 left-20 h-64 w-64 bg-blue-500/25" />

      <Navbar
        userName={user.name}
        userRole={user.role}
        onLogout={handleLogout}
      />

      <main className="relative z-10">
        <section className="w-full px-6 pb-12 mt-20 md:px-12 lg:px-20">
          <div className="rounded-3xl border border-white/15 bg-white/75 p-4 text-slate-900 shadow-2xl backdrop-blur md:p-8">
            <ProductCatalog userRole={user.role} userId={user.id} />
          </div>
        </section>
      </main>
    <div className="min-h-screen p-6 bg-slate-50">
      <header className="flex items-center justify-between max-w-5xl mx-auto mb-6">
        <div>
          <h1 className="text-2xl font-bold">BlackTie Catalog</h1>
          <p className="text-sm text-muted-foreground">
            Welcome, {user.name} ({user.role})
          </p>
        </div>
        <div className="flex gap-3 items-center">
          <NotificationBell userId={user.id} />
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
