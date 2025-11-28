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