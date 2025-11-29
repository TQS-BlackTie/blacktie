import { useEffect, useState } from 'react'
import { RoleSelectionModal } from '../components/role-selection-modal'
import { getCurrentUser, type User } from '../lib/api'

export default function RoleSetupPage() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  
  // Simular userId (em produção viria do token/sessão)
  const userId = Number(localStorage.getItem("userId")) || 1

  useEffect(() => {
    fetchUser()
  }, [])

  const fetchUser = async () => {
    try {
      const userData = await getCurrentUser(userId)
      setUser(userData)
      
      // Se já escolheu o papel (não é mais "renter" default), redireciona
      if (userData.role !== "renter") {
        window.location.href = "/"
      }
    } catch (err) {
      setError("Erro ao carregar utilizador")
    } finally {
      setLoading(false)
    }
  }

  const handleRoleSelected = (role: string) => {
    // Atualizar estado local
    if (user) {
      setUser({ ...user, role })
    }
    
    // Redirecionar para a página principal
    setTimeout(() => {
      window.location.href = "/"
    }, 1000)
  }

  if (loading) {
    return (
      <div className="flex min-h-svh w-full items-center justify-center p-6 md:p-10">
        <div>A carregar...</div>
      </div>
    )
  }

  if (error || !user) {
    return (
      <div className="flex min-h-svh w-full items-center justify-center p-6 md:p-10">
        <div className="text-red-600">{error || "Utilizador não encontrado"}</div>
      </div>
    )
  }

  // Mostrar modal apenas se o papel ainda for "renter" (default)
  if (user.role === "renter") {
    return <RoleSelectionModal userId={user.id} onRoleSelected={handleRoleSelected} />
  }

  return (
    <div className="flex min-h-svh w-full items-center justify-center p-6 md:p-10">
      <div>Papel já definido: {user.role}</div>
    </div>
  )
}
