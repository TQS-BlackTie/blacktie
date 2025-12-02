import { useEffect, useState } from 'react'
import { getCurrentUser, updateUserProfile, type User, type UpdateProfileInput } from '../lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { RoleSelectionModal } from '@/components/role-selection-modal'

export default function ProfilePage() {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [showRoleModal, setShowRoleModal] = useState(false)

  const [formData, setFormData] = useState({
    name: "",
    phone: "",
    address: "",
    businessInfo: ""
  })

  const userId = (() => {
    const userData = localStorage.getItem('user')
    if (userData) {
      try {
        return JSON.parse(userData).id
      } catch {
        return null
      }
    }
    return null
  })()

  useEffect(() => {
    async function fetchUser() {
      if (!userId) {
        window.location.href = '/login'
        return
      }

      try {
        const userData = await getCurrentUser(userId)
        setUser(userData)
        setFormData({
          name: userData.name || "",
          phone: userData.phone || "",
          address: userData.address || "",
          businessInfo: userData.businessInfo || ""
        })
      } catch (e) {
        console.error(e)
        setError("Failed to load user profile")
      } finally {
        setLoading(false)
      }
    }

    fetchUser()
  }, [userId])

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setFormData({
      ...formData,
      [e.target.id]: e.target.value,
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setSuccess("")

    if (!userId) {
      setError("User not authenticated")
      return
    }

    setIsSubmitting(true)

    try {
      const updateData: UpdateProfileInput = {
        name: formData.name,
        phone: formData.phone || undefined,
        address: formData.address || undefined,
        businessInfo: formData.businessInfo || undefined,
      }

      const updatedUser = await updateUserProfile(userId, updateData)
      setUser(updatedUser)
      
      try {
        localStorage.setItem('user', JSON.stringify(updatedUser))
      } catch {
        // ignore
      }

      setSuccess("Profile updated successfully!")
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Failed to update profile"
      setError(errorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleRoleSelected = (_newRole: string) => {
    setShowRoleModal(false)
    const userData = localStorage.getItem('user')
    if (userData) {
      try {
        const updatedUser = JSON.parse(userData)
        setUser(updatedUser)
        setSuccess("Role updated successfully!")
      } catch {
        // reload user from API
        getCurrentUser(userId).then(setUser).catch(console.error)
      }
    }
  }

  const handleBack = () => {
    window.location.href = '/'
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <div>Loading...</div>
      </div>
    )
  }

  if (error && !user) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <div className="text-red-600">{error}</div>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6 bg-slate-50">
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle>Manage Profile</CardTitle>
          <CardDescription>
            Update your personal information and preferences
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
              {success && (
                <div className="text-green-600 text-sm mb-4 p-2 bg-green-50 rounded">
                  {success}
                </div>
              )}

              <Field>
                <FieldLabel htmlFor="name">Full Name</FieldLabel>
                <Input
                  id="name"
                  type="text"
                  value={formData.name}
                  onChange={handleChange}
                  required
                />
              </Field>

              <Field>
                <FieldLabel htmlFor="phone">Phone Number</FieldLabel>
                <Input
                  id="phone"
                  type="tel"
                  placeholder="+351 912 345 678"
                  value={formData.phone}
                  onChange={handleChange}
                />
              </Field>

              <Field>
                <FieldLabel htmlFor="address">Address</FieldLabel>
                <Input
                  id="address"
                  type="text"
                  placeholder="Street, City, Postal Code"
                  value={formData.address}
                  onChange={handleChange}
                />
              </Field>

              {user?.role === "owner" && (
                <Field>
                  <FieldLabel htmlFor="businessInfo">Business Information</FieldLabel>
                  <textarea
                    id="businessInfo"
                    className="min-h-[100px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                    placeholder="Describe your business, rental terms, or special services..."
                    value={formData.businessInfo}
                    onChange={handleChange}
                  />
                </Field>
              )}

              <Field>
                <FieldLabel>Current Role</FieldLabel>
                <div className="flex items-center gap-3 mt-2">
                  <span className="px-3 py-2 bg-gray-100 rounded-md font-medium capitalize">
                    {user?.role || "Not set"}
                  </span>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setShowRoleModal(true)}
                  >
                    Change Role
                  </Button>
                </div>
              </Field>

              <div className="flex gap-3 mt-6">
                <Button type="submit" disabled={isSubmitting} className="flex-1">
                  {isSubmitting ? "Saving..." : "Save Changes"}
                </Button>
              </div>

              <div className="mt-3">
                <Button
                  type="button"
                  onClick={handleBack}
                  variant="ghost"
                  className="w-full"
                >
                  Back to Home
                </Button>
              </div>
            </FieldGroup>
          </form>
        </CardContent>
      </Card>

      {showRoleModal && userId && (
        <RoleSelectionModal
          userId={userId}
          onRoleSelected={handleRoleSelected}
        />
      )}
    </div>
  )
}
