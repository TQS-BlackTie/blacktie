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
            <ProductCatalog userRole={user.role} userId={user.id} showReviews={false} />
          </div>
        </section>
      </main>
    </div>
  )
}
