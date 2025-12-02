export type Product = {
  id: number
  name: string
  description: string
  price: number
  available: boolean
}

export type GetProductsParams = {
  name?: string
  maxPrice?: number
}

export async function getProducts(params: GetProductsParams = {}): Promise<Product[]> {
  const query = new URLSearchParams()

  if (params.name && params.name.trim() !== "") {
    query.set("name", params.name.trim())
  }
  if (params.maxPrice != null) {
    query.set("maxPrice", String(params.maxPrice))
  }

  const url = "/api/products" + (query.toString() ? `?${query.toString()}` : "")

  const res = await fetch(url)
  if (!res.ok) {
    throw new Error("Failed to fetch products")
  }
  return res.json()
}

export type CreateProductInput = {
  name: string
  description: string
  price: number
}

export async function createProduct(input: CreateProductInput): Promise<Product> {
  const res = await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  })

  if (!res.ok) {
    throw new Error("Failed to create product")
  }

  return res.json()
}

// User types and functions
export type User = {
  id: number
  name: string
  email: string
  role: string
  phone?: string
  address?: string
  businessInfo?: string
  createdAt: string
}

export type SetRoleInput = {
  role: "renter" | "owner"
}

export async function getCurrentUser(userId: number): Promise<User> {
  const res = await fetch(`/api/users/${userId}/profile`)

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to fetch user" }))
    throw new Error(error.message || "Failed to fetch user")
  }

  return res.json()
}

export async function setUserRole(userId: number, role: "renter" | "owner"): Promise<User> {
  const res = await fetch(`/api/users/${userId}/role`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ role }),
  })

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to set role" }))
    throw new Error(error.message || "Failed to set role")
  }

  return res.json()
}

export type UpdateProfileInput = {
  name: string
  phone?: string
  address?: string
  businessInfo?: string
}

export async function updateUserProfile(userId: number, data: UpdateProfileInput): Promise<User> {
  const res = await fetch(`/api/users/${userId}/profile`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(data),
  })

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to update profile" }))
    throw new Error(error.message || "Failed to update profile")
  }

  return res.json()
}
