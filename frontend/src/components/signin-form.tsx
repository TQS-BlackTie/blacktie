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
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"

export function SigninForm({ ...props }: React.ComponentProps<typeof Card>) {
  const [formData, setFormData] = useState({
    email: "",
    password: ""
  })
  const [error, setError] = useState("")
  const [success, setSuccess] = useState("")

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setSuccess("")

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: formData.email,
          password: formData.password,
        }),
      })

      if (response.ok) {
        const data = await response.json()
        setSuccess("Logged in successfully!")
        
        // Store user data in localStorage
        localStorage.setItem("userId", data.id)
        localStorage.setItem("user", JSON.stringify({
          id: data.id,
          name: data.name,
          email: data.email,
          role: data.role
        }))

        // Always redirect to role setup after login
        setTimeout(() => {
          window.location.href = "/role-setup"
        }, 1000)
      } else {
        const errorData = await response.json()
        setError(errorData.message || "Failed to log in")
      }
    } catch {
      setError("An error occurred. Please try again.")
    }
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.id]: e.target.value,
    })
  }

  return (
    <Card {...props}>
      <CardHeader>
        <CardTitle>Log In</CardTitle>
        <CardDescription>
          Enter your information below to log into your account
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
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                type="email"
                placeholder="m@example.com"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="password">Password</FieldLabel>
              <Input
                id="password"
                type="password"
                value={formData.password}
                onChange={handleChange}
                required
              />
            </Field>
            <FieldGroup>
              <Field>
                <Button type="submit">Log In</Button>

                <FieldDescription className="px-6 text-center">
                  Don't have an account? <a href="/signup">Sign up</a>
                </FieldDescription>
              </Field>
            </FieldGroup>
          </FieldGroup>
        </form>
      </CardContent>
    </Card>
  )
}
