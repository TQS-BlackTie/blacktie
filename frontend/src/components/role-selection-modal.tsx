import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Field,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { setUserRole } from "@/lib/api"

type RoleSelectionModalProps = {
  userId: number
  onRoleSelected: (role: string) => void
}

export function RoleSelectionModal({ userId, onRoleSelected }: RoleSelectionModalProps) {
  const [selectedRole, setSelectedRole] = useState<"renter" | "owner" | null>(null)
  const [error, setError] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")

    if (!selectedRole) {
      setError("Por favor, escolha um papel")
      return
    }

    setIsSubmitting(true)

    try {
      const updatedUser = await setUserRole(userId, selectedRole)
      onRoleSelected(updatedUser.role)
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Erro ao definir papel"
      setError(errorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-6 z-50">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Escolha o seu papel</CardTitle>
          <CardDescription>
            Selecione como deseja utilizar a plataforma. Esta escolha só pode ser feita uma vez.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit}>
            <FieldGroup>
              {error && (
                <div className="text-red-600 text-sm mb-4 p-2 bg-red-50 rounded">
                  {error}
                </div>
              )}

              <Field>
                <FieldLabel>Selecione o seu papel:</FieldLabel>
                
                <div className="space-y-3 mt-2">
                  <label className="flex items-start p-4 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors">
                    <input
                      type="radio"
                      name="role"
                      value="renter"
                      checked={selectedRole === "renter"}
                      onChange={() => setSelectedRole("renter")}
                      className="mt-1 mr-3"
                    />
                    <div>
                      <div className="font-medium">Renter (Arrendatário)</div>
                      <div className="text-sm text-gray-600">
                        Pretendo alugar fatos para eventos
                      </div>
                    </div>
                  </label>

                  <label className="flex items-start p-4 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors">
                    <input
                      type="radio"
                      name="role"
                      value="owner"
                      checked={selectedRole === "owner"}
                      onChange={() => setSelectedRole("owner")}
                      className="mt-1 mr-3"
                    />
                    <div>
                      <div className="font-medium">Owner (Proprietário)</div>
                      <div className="text-sm text-gray-600">
                        Pretendo disponibilizar fatos para aluguer
                      </div>
                    </div>
                  </label>
                </div>
              </Field>

              <div className="mt-6">
                <Button type="submit" disabled={isSubmitting || !selectedRole} className="w-full">
                  {isSubmitting ? "A guardar..." : "Confirmar escolha"}
                </Button>
              </div>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
