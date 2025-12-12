import { useEffect, useState } from "react"
import { getProducts, createProduct, getReviewsByProduct, type Product } from "@/lib/api"
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
  const [ratingsMap, setRatingsMap] = useState<Record<number, { avg: number; count: number }>>({})

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
      // fetch reviews for products to compute average rating
      try {
        const map: Record<number, { avg: number; count: number }> = {}
        await Promise.all(data.map(async (p) => {
          try {
            const revs = await getReviewsByProduct(p.id)
            if (revs && revs.length > 0) {
              const sum = revs.reduce((s, r) => s + r.rating, 0)
              map[p.id] = { avg: sum / revs.length, count: revs.length }
            } else {
              map[p.id] = { avg: 0, count: 0 }
            }
          } catch {
            map[p.id] = { avg: 0, count: 0 }
          }
        }))
        setRatingsMap(map)
      } catch {
        // ignore review fetch errors
      }
    } catch {
      setError("Failed to load products")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadProducts()
    // listen for reviews created elsewhere to refresh affected product rating
    const handler = async (e: any) => {
      try {
        const pid = e?.detail?.productId
        if (!pid) return
        const revs = await getReviewsByProduct(pid)
        setRatingsMap(prev => ({ ...prev, [pid]: revs && revs.length > 0 ? { avg: revs.reduce((s, r) => s + r.rating, 0) / revs.length, count: revs.length } : { avg: 0, count: 0 } }))
      } catch {
        // ignore
      }
    }
    window.addEventListener('review:created', handler as EventListener)
    return () => { window.removeEventListener('review:created', handler as EventListener) }
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
              {ratingsMap[p.id] && ratingsMap[p.id].count > 0 && (
                <p className="text-sm text-yellow-600 mb-2">Average: {ratingsMap[p.id].avg.toFixed(1)} ⭐ ({ratingsMap[p.id].count})</p>
              )}
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
