import { useEffect, useState } from 'react'
import { getCurrentUser, updateUserProfile, type User, type UpdateProfileInput } from '../lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { RoleSelectionModal } from '@/components/role-selection-modal'
import { Navbar } from '@/components/navbar'
import { NotificationBell } from '@/components/notification-bell'

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
      } catch (err: unknown) {
        console.error(err)
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
      window.location.href = '/'
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : "Failed to update profile"
      setError(errorMessage)
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('user')
    window.location.href = '/login'
  }

  const handleBack = () => {
    window.location.href = '/'
  }

  const handleRoleSelected = () => {
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

  return (
    <div className="relative min-h-screen overflow-hidden text-slate-50">
      <div className="aurora-blob -left-10 top-12 h-64 w-64 bg-emerald-500/40" />
      <div className="aurora-blob delay-1 right-0 top-48 h-64 w-64 bg-cyan-400/30" />
      <div className="aurora-blob delay-2 -bottom-6 left-20 h-64 w-64 bg-blue-500/25" />

      <Navbar
        userName={user?.name || "Profile"}
        userRole={user?.role}
        onLogout={handleLogout}
        notificationBell={userId ? <NotificationBell userId={userId} /> : null}
      />

      <main className="relative z-10 w-full px-6 pb-12 pt-10">
        {loading ? (
          <div className="flex min-h-[60vh] items-center justify-center">
            <div className="rounded-full border border-white/20 px-4 py-2 text-sm text-slate-200">
              Loading profile...
            </div>
          </div>
        ) : error && !user ? (
          <div className="flex min-h-[60vh] items-center justify-center">
            <div className="rounded-xl border border-red-200/40 bg-red-50/80 px-4 py-3 text-sm font-medium text-red-700 shadow">
              {error}
            </div>
          </div>
        ) : (
          <Card className="fade-up w-full rounded-3xl border-white/10 bg-white/80 text-slate-900 shadow-2xl backdrop-blur">
            <CardHeader className="pb-4 md:pb-6">
              <CardTitle>Manage Profile</CardTitle>
              <CardDescription>
                Update your personal information and preferences
              </CardDescription>
            </CardHeader>
            <CardContent className="pt-0 md:pt-2">
              <form onSubmit={handleSubmit}>
                <FieldGroup>
                  {error && (
                    <div className="mb-4 rounded border border-red-100 bg-red-50 p-3 text-sm text-red-700">
                      {error}
                    </div>
                  )}
                  {success && (
                    <div className="mb-4 rounded border border-emerald-100 bg-emerald-50 p-3 text-sm text-emerald-700">
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
                      className="rounded-xl"
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
                      className="rounded-xl"
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
                      className="rounded-xl"
                    />
                  </Field>

                  {user?.role === "owner" && (
                    <Field>
                      <FieldLabel htmlFor="businessInfo">Business Information</FieldLabel>
                      <textarea
                        id="businessInfo"
                        className="min-h-[100px] w-full rounded-xl border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                        placeholder="Describe your business, rental terms, or special services..."
                        value={formData.businessInfo}
                        onChange={handleChange}
                      />
                    </Field>
                  )}

                  <Field>
                    <FieldLabel>Current Role</FieldLabel>
                    <div className="mt-2 flex items-center gap-3">
                      <span className="rounded-xl bg-gray-100 px-3 py-2 font-medium capitalize">
                        {user?.role || "Not set"}
                      </span>
                      <Button
                        type="button"
                        variant="outline"
                        onClick={() => setShowRoleModal(true)}
                        className="rounded-full"
                      >
                        Change Role
                      </Button>
                    </div>
                  </Field>

                  <div className="mt-6 flex flex-col gap-3 sm:flex-row">
                    <Button type="submit" disabled={isSubmitting} className="flex-1 rounded-full">
                      {isSubmitting ? "Saving..." : "Save Changes"}
                    </Button>
                    <Button
                      type="button"
                      onClick={handleBack}
                      variant="ghost"
                      className="flex-1 rounded-full"
                    >
                      Back to Home
                    </Button>
                  </div>
                </FieldGroup>
              </form>
            </CardContent>
          </Card>
        )}
      </main>

      {showRoleModal && userId && (
        <RoleSelectionModal
          userId={userId}
          onRoleSelected={handleRoleSelected}
        />
      )}
    </div>
  )
}
