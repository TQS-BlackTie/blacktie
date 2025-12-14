export type Product = {
  id: number
  name: string
  description: string
  price: number
  available: boolean
  depositAmount?: number
  imageUrl?: string
  address?: string
  city?: string
  postalCode?: string
  latitude?: number
  longitude?: number
  size?: string
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
  depositAmount?: number
  image?: File
  address?: string
  city?: string
  postalCode?: string
  latitude?: number
  longitude?: number
  size?: string
}

export async function createProduct(userId: number, input: CreateProductInput): Promise<Product> {
  // If there's an image, use multipart form data
  if (input.image) {
    const formData = new FormData()
    formData.append("name", input.name)
    formData.append("description", input.description)
    formData.append("price", String(input.price))
    if (input.depositAmount != null) {
      formData.append("depositAmount", String(input.depositAmount))
    }
    if (input.address) {
      formData.append("address", input.address)
    }
    if (input.city) {
      formData.append("city", input.city)
    }
    if (input.postalCode) {
      formData.append("postalCode", input.postalCode)
    }
    if (input.latitude != null) {
      formData.append("latitude", String(input.latitude))
    }
    if (input.longitude != null) {
      formData.append("longitude", String(input.longitude))
    }
    if (input.size) {
      formData.append("size", input.size)
    }
    formData.append("image", input.image)

    const res = await fetch("/api/products/with-image", {
      method: "POST",
      headers: { "X-User-Id": String(userId) },
      body: formData,
    })

    if (!res.ok) {
      throw new Error("Failed to create product")
    }

    return res.json()
  }

  // Otherwise use JSON
  const res = await fetch("/api/products", {
    method: "POST",
    headers: { "Content-Type": "application/json", "X-User-Id": String(userId) },
    body: JSON.stringify({
      name: input.name,
      description: input.description,
      price: input.price,
      depositAmount: input.depositAmount,
      address: input.address,
      city: input.city,
      postalCode: input.postalCode,
      latitude: input.latitude,
      longitude: input.longitude,
      size: input.size,
    }),
  })

  if (!res.ok) {
    throw new Error("Failed to create product")
  }

  return res.json()
}

export async function deleteProduct(userId: number, productId: number): Promise<void> {
  const res = await fetch(`/api/products/${productId}`, {
    method: "DELETE",
    headers: { "X-User-Id": String(userId) },
  })

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to delete product' }))
    throw new Error(error.message || 'Failed to delete product')
  }
}

// Cache for Portuguese municipalities
let municipalitiesCache: string[] | null = null
let municipalitiesFetchPromise: Promise<string[]> | null = null

// Fetch all Portuguese municipalities from geoapi.pt
async function fetchAllMunicipalities(): Promise<string[]> {
  if (municipalitiesCache) {
    return municipalitiesCache
  }

  // If already fetching, return the existing promise
  if (municipalitiesFetchPromise) {
    return municipalitiesFetchPromise
  }

  municipalitiesFetchPromise = (async () => {
    try {
      const res = await fetch('https://json.geoapi.pt/municipios')
      if (!res.ok) {
        return []
      }
      const data = await res.json()

      // The API returns a plain JSON array of municipality names
      if (Array.isArray(data)) {
        municipalitiesCache = data
        return data
      }

      return []
    } catch {
      return []
    } finally {
      municipalitiesFetchPromise = null
    }
  })()

  return municipalitiesFetchPromise
}

// Get municipality suggestions based on query (filtered locally)
export async function getPortugueseMunicipalities(query: string): Promise<string[]> {
  if (!query || query.length < 2) {
    return []
  }

  const allMunicipalities = await fetchAllMunicipalities()
  const normalizedQuery = query.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')

  // Filter municipalities that match the query (case-insensitive, accent-insensitive)
  const matches = allMunicipalities.filter(municipality => {
    const normalizedMunicipality = municipality.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    return normalizedMunicipality.includes(normalizedQuery)
  })

  // Sort: prioritize matches that start with the query
  matches.sort((a, b) => {
    const normalizedA = a.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    const normalizedB = b.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    const aStartsWith = normalizedA.startsWith(normalizedQuery)
    const bStartsWith = normalizedB.startsWith(normalizedQuery)

    if (aStartsWith && !bStartsWith) return -1
    if (!aStartsWith && bStartsWith) return 1
    return a.localeCompare(b, 'pt')
  })

  return matches.slice(0, 10)
}

// Validate if a string is a valid Portuguese municipality
export async function isValidPortugueseMunicipality(name: string): Promise<boolean> {
  if (!name || name.trim().length === 0) {
    return false
  }

  const allMunicipalities = await fetchAllMunicipalities()
  const normalizedName = name.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')

  return allMunicipalities.some(municipality => {
    const normalizedMunicipality = municipality.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    return normalizedMunicipality === normalizedName
  })
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
  averageRating?: number
  totalReviews?: number
  renterAverageRating?: number
  renterReviewCount?: number
  ownerAverageRating?: number
  ownerReviewCount?: number
}

export type SetRoleInput = {
  role: "renter" | "owner"
}

export async function getCurrentUser(userId: number): Promise<User> {
  const res = await fetch(`/api/users/${userId}`)

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to fetch user" }))
    throw new Error(error.message || "Failed to fetch user")
  }

  return res.json()
}

export async function setUserRole(userId: number, role: "renter" | "owner"): Promise<User> {
  const res = await fetch(`/api/users/${userId}/role`, {
    method: "PUT",
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

export async function getUserReputation(userId: number): Promise<User> {
  const res = await fetch(`/api/users/${userId}/reputation`)
  if (!res.ok) {
    throw new Error("Failed to fetch user reputation")
  }
  return res.json()
}

export async function updateUserProfile(userId: number, data: UpdateProfileInput): Promise<User> {
  const res = await fetch(`/api/users/${userId}`, {
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
  deliveryMethod?: string
  deliveryCode?: string
  pickupLocation?: string
  rejectionReason?: string
  approvedAt?: string
  paidAt?: string
  depositAmount?: number
  depositRequested?: boolean
  depositReason?: string
  depositRequestedAt?: string
  depositPaid?: boolean
  depositPaidAt?: string
  productDepositAmount?: number // The security deposit from the product
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

// Owner booking management
export async function getPendingApprovalBookings(ownerId: number): Promise<Booking[]> {
  const res = await fetch("/api/bookings/pending-approval", {
    headers: { "X-User-Id": String(ownerId) },
  })

  if (!res.ok) {
    throw new Error("Failed to fetch pending bookings")
  }

  return res.json()
}

export type ApproveBookingInput = {
  deliveryMethod: 'PICKUP' | 'SHIPPING'
  pickupLocation?: string
}

export async function approveBooking(
  ownerId: number,
  bookingId: number,
  input: ApproveBookingInput
): Promise<Booking> {
  const res = await fetch(`/api/bookings/${bookingId}/approve`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": String(ownerId),
    },
    body: JSON.stringify(input),
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to approve booking")
  }

  return res.json()
}

export type RejectBookingInput = {
  reason?: string
}

export async function rejectBooking(
  ownerId: number,
  bookingId: number,
  input: RejectBookingInput
): Promise<Booking> {
  const res = await fetch(`/api/bookings/${bookingId}/reject`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": String(ownerId),
    },
    body: JSON.stringify(input),
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to reject booking")
  }

  return res.json()
}

export async function processBookingPayment(userId: number, bookingId: number): Promise<Booking> {
  const res = await fetch(`/api/bookings/${bookingId}/payment`, {
    method: "POST",
    headers: {
      "X-User-Id": String(userId),
    },
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to process payment")
  }

  return res.json()
}

export type RequestDepositInput = {
  depositAmount: number
  reason: string
}

export async function requestDeposit(
  ownerId: number,
  bookingId: number,
  input: RequestDepositInput
): Promise<Booking> {
  const res = await fetch(`/api/bookings/${bookingId}/request-deposit`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": String(ownerId),
    },
    body: JSON.stringify(input),
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to request deposit")
  }

  return res.json()
}

export async function payDeposit(userId: number, bookingId: number): Promise<Booking> {
  const res = await fetch(`/api/bookings/${bookingId}/pay-deposit`, {
    method: "POST",
    headers: {
      "X-User-Id": String(userId),
    },
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to pay deposit")
  }

  return res.json()
}

export async function refundDeposit(userId: number, bookingId: number): Promise<Booking> {
  const res = await fetch(`/api/bookings/${bookingId}/refund-deposit`, {
    method: "POST",
    headers: {
      "X-User-Id": String(userId),
    },
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to refund deposit")
  }

  return res.json()
}

// Payment types and functions
export type PaymentIntentRequest = {
  bookingId: number
  amount: number // amount in cents
}

export type PaymentIntentResponse = {
  clientSecret: string
  paymentIntentId: string
  amount: number
  currency: string
}

export async function createPaymentIntent(
  userId: number,
  request: PaymentIntentRequest
): Promise<PaymentIntentResponse> {
  const res = await fetch("/api/payments/create-payment-intent", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-User-Id": String(userId),
    },
    body: JSON.stringify(request),
  })

  if (!res.ok) {
    const error = await res.text()
    throw new Error(error || "Failed to create payment intent")
  }

  return res.json()
}

export async function getPaymentStatus(paymentIntentId: string): Promise<{ status: string }> {
  const res = await fetch(`/api/payments/status/${paymentIntentId}`)

  if (!res.ok) {
    throw new Error("Failed to fetch payment status")
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

// Reviews
export type ReviewResponse = {
  id: number
  bookingId: number
  productId?: number
  rating: number
  comment?: string
  createdAt: string
  reviewType?: 'OWNER' | 'RENTER'
}

export async function getReviewByBooking(bookingId: number): Promise<ReviewResponse | null> {
  const res = await fetch(`/api/reviews/booking/${bookingId}`)
  if (res.status === 404) return null
  if (!res.ok) throw new Error('Failed to fetch review')
  return res.json()
}

export async function createReview(userId: number, bookingId: number, rating: number, comment?: string): Promise<ReviewResponse> {
  const form = new FormData()
  form.append('bookingId', String(bookingId))
  form.append('rating', String(rating))
  if (comment) form.append('comment', comment)

  const res = await fetch('/api/reviews', {
    method: 'POST',
    headers: { 'X-User-Id': String(userId) },
    body: form,
  })

  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || 'Failed to create review')
  }

  return res.json()
}

export async function getReviewsByProduct(productId: number): Promise<ReviewResponse[]> {
  const res = await fetch(`/api/reviews/product/${productId}`)
  if (!res.ok) throw new Error('Failed to fetch reviews')
  return res.json()
}

// Admin types and functions
export type PlatformMetrics = {
  totalUsers: number
  totalOwners: number
  totalRenters: number
  totalProducts: number
  availableProducts: number
  totalBookings: number
  activeBookings: number
  completedBookings: number
  cancelledBookings: number
  totalRevenue: number
  averageBookingValue: number
}

export type AdminUser = {
  id: number
  name: string
  email: string
  role: string
  status: string
  phone?: string
  address?: string
  businessInfo?: string
  createdAt: string
  bookingsCount: number
  productsCount: number
}

export async function getAdminMetrics(userId: number): Promise<PlatformMetrics> {
  const res = await fetch('/api/admin/metrics', {
    headers: { 'X-User-Id': String(userId) }
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to fetch metrics' }))
    throw new Error(error.message || 'Failed to fetch metrics')
  }
  return res.json()
}

export async function getAdminUsers(userId: number): Promise<AdminUser[]> {
  const res = await fetch('/api/admin/users', {
    headers: { 'X-User-Id': String(userId) }
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to fetch users' }))
    throw new Error(error.message || 'Failed to fetch users')
  }
  return res.json()
}

export async function updateUserStatus(adminId: number, targetUserId: number, status: string): Promise<AdminUser> {
  const res = await fetch(`/api/admin/users/${targetUserId}/status`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': String(adminId)
    },
    body: JSON.stringify({ status })
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to update status' }))
    throw new Error(error.message || 'Failed to update status')
  }
  return res.json()
}

export async function updateUserRoleAdmin(adminId: number, targetUserId: number, role: string): Promise<AdminUser> {
  const res = await fetch(`/api/admin/users/${targetUserId}/role`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'X-User-Id': String(adminId)
    },
    body: JSON.stringify({ role })
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to update role' }))
    throw new Error(error.message || 'Failed to update role')
  }
  return res.json()
}

export async function deleteUserAdmin(adminId: number, targetUserId: number): Promise<void> {
  const res = await fetch(`/api/admin/users/${targetUserId}`, {
    method: 'DELETE',
    headers: { 'X-User-Id': String(adminId) }
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to delete user' }))
    throw new Error(error.message || 'Failed to delete user')
  }
}

// Admin Product Management
export type AdminProduct = {
  id: number
  name: string
  description: string
  price: number
  available: boolean
  owner: {
    id: number
    name: string
    email: string
  }
}

export async function getAdminProducts(userId: number): Promise<AdminProduct[]> {
  const res = await fetch('/api/admin/products', {
    headers: { 'X-User-Id': String(userId) }
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to fetch products' }))
    throw new Error(error.message || 'Failed to fetch products')
  }
  return res.json()
}

export async function deleteProductAdmin(adminId: number, productId: number): Promise<void> {
  const res = await fetch(`/api/admin/products/${productId}`, {
    method: 'DELETE',
    headers: { 'X-User-Id': String(adminId) }
  })
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to delete product' }))
    throw new Error(error.message || 'Failed to delete product')
  }
}
