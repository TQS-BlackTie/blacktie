import { useEffect, useState } from 'react'
import { RoleSelectionModal } from '../components/role-selection-modal'
import { getCurrentUser, type User } from '../lib/api'

export default function RoleSetupPage() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  // Get userId from localStorage (set after login)
  const userId = Number(localStorage.getItem("userId")) || 1
  useEffect(() => {
    async function fetchUser() {
      try {
        const userData = await getCurrentUser(userId)
        setUser(userData)
      } catch (err: unknown) {
        console.error(err)
        setError("Erro ao carregar utilizador")
      } finally {
        setLoading(false)
      }
    }

    if (userId) {
      fetchUser()
    } else {
      setLoading(false)
    }
  }, [userId])

  const handleRoleSelected = async (role: string) => {
    if (user) {
      setUser({ ...user, role })

      setTimeout(() => {
        window.location.href = "/"
      }, 500)
    }
  }

  if (loading) {
    return (
      <div className="flex min-h-svh w-full items-center justify-center p-6 md:p-10">
        <div>Loading...</div>
      </div>
    )
  }

  if (error || !user) {
    return (
      <div className="flex min-h-svh w-full items-center justify-center p-6 md:p-10">
        <div className="text-red-600">{error || "User not found"}</div>
      </div>
    )
  }



  return <RoleSelectionModal
    userId={user.id}
    onRoleSelected={handleRoleSelected}
    title="Complete Account Setup"
  />
}
