import { useEffect, useState } from 'react'

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
    <div className="flex min-h-screen flex-col items-center justify-center p-6">
      <div className="w-full max-w-md space-y-6">
        <div className="rounded-lg border bg-card p-8 text-center shadow-sm">
          <h1 className="text-3xl font-bold mb-2">
            Hello, {user.name}!
          </h1>
          <p className="text-lg text-muted-foreground mb-6">
            ({user.role})
          </p>
          <div className="space-y-2 text-sm text-left">
            <p><span className="font-semibold">Email:</span> {user.email}</p>
            <p><span className="font-semibold">Role:</span> {user.role}</p>
          </div>
          <button
            onClick={handleLogout}
            className="mt-6 w-full rounded-md bg-primary px-4 py-2 text-primary-foreground hover:bg-primary/90"
          >
            Logout
          </button>
        </div>
      </div>
    </div>
  )
}
