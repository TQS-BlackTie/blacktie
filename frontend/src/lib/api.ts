export type Product = {
  id: number
  name: string
  description: string
  price: number
  available: boolean
  owner?: {
    id: number
    name?: string
    email?: string
  }
}

export type GetProductsParams = {
  name?: string
  maxPrice?: number
  userId: number
}

export async function getProducts(params: GetProductsParams): Promise<Product[]> {
  const query = new URLSearchParams()

  if (params.name && params.name.trim() !== "") {
    query.set("name", params.name.trim())
  }
  if (params.maxPrice != null) {
    query.set("maxPrice", String(params.maxPrice))
  }

  const url = "/api/products" + (query.toString() ? `?${query.toString()}` : "")

  const res = await fetch(url, {
    headers: { "X-User-Id": String(params.userId) },
  })
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

export async function createProduct(userId: number, input: CreateProductInput): Promise<Product> {
  const res = await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-User-Id": String(userId) },
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

// Booking types and functions
export type Booking = {
  id: number
  renterId: number
  renterName: string
  productId: number
  productName: string
  ownerId?: number
  ownerName?: string
  bookingDate: string
  returnDate: string
  totalPrice: number
  status: string
}

export type CreateBookingInput = {
  productId: number
  bookingDate: string
  returnDate: string
}

export async function createBooking(userId: number, input: CreateBookingInput): Promise<Booking> {
  const res = await fetch("/api/bookings", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": String(userId),
    },
    body: JSON.stringify(input),
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to create booking")
  }

  return res.json()
}

export async function getUserBookings(userId: number): Promise<Booking[]> {
  const res = await fetch(`/api/bookings/user/${userId}`)

  if (!res.ok) {
    throw new Error("Failed to fetch bookings")
  }

  return res.json()
}

export async function cancelBooking(userId: number, bookingId: number): Promise<void> {
  const res = await fetch(`/api/bookings/${bookingId}`, {
    method: "DELETE",
    headers: {
      "X-User-Id": String(userId),
    },
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to cancel booking")
  }
}

export async function getBookingsByProduct(productId: number, userId: number): Promise<Booking[]> {
  const res = await fetch(`/api/bookings/product/${productId}`, {
    headers: { "X-User-Id": String(userId) },
  })

  if (!res.ok) {
    throw new Error("Failed to fetch bookings for product")
  }

  return res.json()
}

export async function getRenterHistory(userId: number): Promise<Booking[]> {
  const res = await fetch(`/api/bookings/user/${userId}/history`)

  if (!res.ok) {
    throw new Error("Failed to fetch booking history")
  }

  return res.json()
}

export async function getActiveBookings(userId: number): Promise<Booking[]> {
  const res = await fetch(`/api/bookings/user/${userId}/active`)

  if (!res.ok) {
    throw new Error("Failed to fetch active bookings")
  }

  return res.json()
}
