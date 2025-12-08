import { useEffect, useState } from "react"
import { getProducts, createProduct, type Product } from "@/lib/api"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { BookingModal } from "@/components/booking-modal"
import { ProductBookingsModal } from "@/components/product-bookings-modal"

type ProductCatalogProps = {
  userRole: string
  userId: number
}

export function ProductCatalog({ userRole, userId }: ProductCatalogProps) {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState("")
  const [maxPrice, setMaxPrice] = useState<string>("")

  const [newName, setNewName] = useState("")
  const [newDescription, setNewDescription] = useState("")
  const [newPrice, setNewPrice] = useState("")
  const [adding, setAdding] = useState(false)
  const [addError, setAddError] = useState<string | null>(null)

  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [manageProduct, setManageProduct] = useState<Product | null>(null)

  const canCreateProduct = userRole === "owner"

  const loadProducts = async () => {
    try {
      setLoading(true)
      setError(null)

      const numericMaxPrice =
        maxPrice.trim() === "" ? undefined : Number(maxPrice)

      const data = await getProducts({
        name: search.trim() || undefined,
        maxPrice:
          numericMaxPrice !== undefined && !Number.isNaN(numericMaxPrice)
            ? numericMaxPrice
            : undefined,
        userId,
      })

      setProducts(data)
    } catch {
      setError("Failed to load products")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadProducts()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    void loadProducts()
  }

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault()
    setAddError(null)

    const priceNumber = Number(newPrice)
    if (!newName.trim() || Number.isNaN(priceNumber) || priceNumber <= 0) {
      setAddError("Please provide a name and a positive price")
      return
    }

    try {
      setAdding(true)
      await createProduct(userId, {
        name: newName.trim(),
        description: newDescription.trim(),
        price: priceNumber,
      })

      setNewName("")
      setNewDescription("")
      setNewPrice("")
      await loadProducts()
    } catch {
      setAddError("Failed to add product")
    } finally {
      setAdding(false)
    }
  }

  return (
    <div className="space-y-4 w-full max-w-3xl mx-auto mt-8">
      <form
        onSubmit={handleSubmit}
        className="flex flex-wrap gap-2 items-end bg-white/60 p-4 rounded-lg shadow"
      >
        <div className="flex flex-col flex-1 min-w-[180px]">
          <label className="text-sm font-medium" htmlFor="search">
            Search by name
          </label>
          <Input
            id="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Smoking, vestido, gravata..."
          />
        </div>

        <div className="flex flex-col w-32">
          <label className="text-sm font-medium" htmlFor="maxPrice">
            Max price
          </label>
          <Input
            id="maxPrice"
            type="number"
            min={0}
            step={1}
            value={maxPrice}
            onChange={(e) => setMaxPrice(e.target.value)}
            placeholder="100"
          />
        </div>

        <Button type="submit" disabled={loading} className="mt-1">
          {loading ? "Loading..." : "Search"}
        </Button>
      </form>

      {canCreateProduct && (
        <form
          onSubmit={handleAdd}
          className="flex flex-col gap-2 bg-white/60 p-4 rounded-lg shadow"
        >
          <h2 className="font-semibold text-lg">Add product</h2>
          {addError && (
            <div className="text-red-600 text-sm bg-red-50 p-2 rounded">
              {addError}
            </div>
          )}
          <div className="flex flex-col md:flex-row gap-2">
            <Input
              placeholder="Name"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="flex-1"
            />
            <Input
              placeholder="Description"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              className="flex-1"
            />
            <Input
              type="number"
              min={0}
              step={1}
              placeholder="Price"
              value={newPrice}
              onChange={(e) => setNewPrice(e.target.value)}
              className="w-32"
            />
            <Button type="submit" disabled={adding}>
              {adding ? "Adding..." : "Add"}
            </Button>
          </div>
        </form>
      )}

      {error && (
        <div className="text-red-600 text-sm bg-red-50 p-2 rounded">
          {error}
        </div>
      )}

      {products.length === 0 && !loading && (
        <div className="text-sm text-gray-500 text-center">
          No products found.
        </div>
      )}

      <div className="grid gap-4 md:grid-cols-3">
        {products.map((p) => (
          <Card key={p.id}>
            <CardHeader>
              <CardTitle>{p.name}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-gray-700 mb-2">{p.description}</p>
              <p className="font-semibold mb-3">{p.price.toFixed(2)} € / day</p>
              <div className="flex flex-col gap-2">
                <Button
                  onClick={() => (canCreateProduct ? setManageProduct(p) : setSelectedProduct(p))}
                  className="w-full"
                  disabled={
                    !canCreateProduct && (!p.available || p.owner?.id === userId)
                  }
                  variant={canCreateProduct ? "outline" : "default"}
                >
                  {canCreateProduct
                    ? "View bookings"
                    : p.owner?.id === userId
                      ? "Your listing"
                      : p.available
                        ? "Reserve"
                        : "Unavailable"}
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {selectedProduct && (
        <BookingModal
          product={selectedProduct}
          userId={userId}
          onClose={() => setSelectedProduct(null)}
          onSuccess={() => {
            setSelectedProduct(null)
            alert("Booking created successfully!")
          }}
        />
      )}

      {manageProduct && (
        <ProductBookingsModal
          product={manageProduct}
          userId={userId}
          onClose={() => setManageProduct(null)}
        />
      )}
    </div>
  )
}
