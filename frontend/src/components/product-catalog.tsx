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
    <div className="mt-4 w-full space-y-6">
      <form
        onSubmit={handleSubmit}
        className="flex flex-wrap items-end gap-3 rounded-2xl border border-slate-200/70 bg-white/80 p-4 shadow-lg backdrop-blur md:p-5"
      >
        <div className="flex min-w-[220px] flex-1 flex-col">
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

        <div className="flex w-32 flex-col">
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

        <Button type="submit" disabled={loading} className="mt-1 rounded-full px-6">
          {loading ? "Loading..." : "Search"}
        </Button>
      </form>

      {canCreateProduct && (
        <form
          onSubmit={handleAdd}
          className="flex flex-col gap-3 rounded-2xl border border-emerald-100/70 bg-gradient-to-r from-emerald-50/80 via-white to-white p-4 shadow-lg backdrop-blur"
        >
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold">Add product</h2>
            <span className="rounded-full bg-white/80 px-3 py-1 text-xs font-medium text-emerald-800 shadow-inner">
              Owner tools
            </span>
          </div>
          {addError && (
            <div className="text-red-600 text-sm bg-red-50 p-2 rounded">
              {addError}
            </div>
          )}
          <div className="flex flex-col gap-2 md:flex-row">
            <Input
              placeholder="Name"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="flex-1 rounded-xl"
            />
            <Input
              placeholder="Description"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              className="flex-1 rounded-xl"
            />
            <Input
              type="number"
              min={0}
              step={1}
              placeholder="Price"
              value={newPrice}
              onChange={(e) => setNewPrice(e.target.value)}
              className="w-32 rounded-xl"
            />
            <Button type="submit" disabled={adding} className="rounded-full">
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

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {products.map((p) => (
          <Card
            key={p.id}
            className="group relative overflow-hidden border-slate-200/80 shadow-lg transition duration-300 hover:-translate-y-1 hover:shadow-2xl"
          >
            <span className="pointer-events-none absolute inset-0 bg-gradient-to-br from-emerald-50 via-transparent to-transparent opacity-0 transition duration-300 group-hover:opacity-100" />
            <CardHeader>
              <CardTitle>{p.name}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="mb-2 text-sm text-gray-700">{p.description}</p>
              <p className="mb-3 font-semibold">{p.price.toFixed(2)} € / day</p>
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
