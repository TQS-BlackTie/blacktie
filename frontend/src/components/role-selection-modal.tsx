import { useState, useEffect } from "react"
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
import { Button } from "@/components/ui/button"
import { setUserRole, getCurrentUser } from "@/lib/api"

type RoleSelectionModalProps = {
  userId: number
  onRoleSelected: (role: string) => void
  onClose?: () => void
  title?: string
}

export function RoleSelectionModal({ userId, onRoleSelected, onClose, title }: RoleSelectionModalProps) {
  const [selectedRole, setSelectedRole] = useState<"renter" | "owner" | null>(null)
  const [currentRole, setCurrentRole] = useState<string | null>(null)
  const [error, setError] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    async function loadCurrentRole() {
      try {
        const user = await getCurrentUser(userId)
        setCurrentRole(user.role)
        // Don't pre-select any role - user must choose
      } catch (err: unknown) {
        console.error("Failed to load current role:", err)
      }
    }
    loadCurrentRole()
  }, [userId])

  const handleConfirm = async () => {
    if (!selectedRole || isSubmitting) return
    setError("")
    setIsSubmitting(true)

    try {
      const updatedUser = await setUserRole(userId, selectedRole)
      localStorage.setItem('user', JSON.stringify(updatedUser))
      onRoleSelected(updatedUser.role)
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Failed to set role"
      setError(errorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-6 z-50">
      <Card className="w-full max-w-md relative">
        {onClose && (
          <button
            onClick={onClose}
            className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors"
            aria-label="Close"
          >
            <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}
        <CardHeader>
          <CardTitle>{title || "Choose Your Role"}</CardTitle>
          <CardDescription>
            {currentRole && currentRole !== "renter"
              ? `Current role: ${currentRole === "owner" ? "Owner" : currentRole}. You can change it anytime.`
              : "Select how you want to use the platform. You can change it anytime."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <FieldGroup>
            {error && (
              <div className="text-red-600 text-sm mb-4 p-2 bg-red-50 rounded">
                {error}
              </div>
            )}

            {isSubmitting && (
              <div className="text-blue-600 text-sm mb-4 p-2 bg-blue-50 rounded">
                Saving...
              </div>
            )}

            <Field>
              <FieldLabel>Select your role:</FieldLabel>

              <div className="space-y-3 mt-2">
                <label className={`flex items-center p-4 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors ${selectedRole === "renter" ? "border-blue-500 bg-blue-50" : ""}`}>
                  <input
                    type="radio"
                    name="role"
                    value="renter"
                    checked={selectedRole === "renter"}
                    onChange={() => setSelectedRole("renter")}
                    disabled={isSubmitting}
                    className="mr-3"
                  />
                  <div className="text-left">
                    <div className="font-medium">Renter</div>
                    <div className="text-sm text-gray-600">
                      I want to rent suits for events
                    </div>
                  </div>
                </label>

                <label className={`flex items-center p-4 border rounded-lg cursor-pointer hover:bg-gray-50 transition-colors ${selectedRole === "owner" ? "border-blue-500 bg-blue-50" : ""}`}>
                  <input
                    type="radio"
                    name="role"
                    value="owner"
                    checked={selectedRole === "owner"}
                    onChange={() => setSelectedRole("owner")}
                    disabled={isSubmitting}
                    className="mr-3"
                  />
                  <div className="text-left">
                    <div className="font-medium">Owner</div>
                    <div className="text-sm text-gray-600">
                      I want to make my suits available for rent
                    </div>
                  </div>
                </label>
              </div>
            </Field>

            <Button
              onClick={handleConfirm}
              disabled={!selectedRole || isSubmitting}
              className="w-full mt-4 bg-green-600 hover:bg-green-700 text-white"
            >
              {isSubmitting ? "Saving..." : "Confirm Role"}
            </Button>



          </FieldGroup>
        </CardContent>
      </Card>
    </div>
  )
}
