import { useEffect, useState } from "react"
import { getProducts, createProduct, getReviewsByProduct, type Product } from "@/lib/api"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { BookingModal } from "@/components/booking-modal"
import { ProductBookingsModal } from "@/components/product-bookings-modal"
import { ProductDetailModal } from "@/components/product-detail-modal"
import UserProfileModal from "./UserProfileModal"

type ProductCatalogProps = {
  userRole: string
  userId: number
  showReviews?: boolean
}

export function ProductCatalog({ userRole, userId, showReviews = true }: ProductCatalogProps) {
  const [products, setProducts] = useState<Product[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [search, setSearch] = useState("")
  const [maxPrice, setMaxPrice] = useState<string>("")

  const [newName, setNewName] = useState("")
  const [newDescription, setNewDescription] = useState("")
  const [newPrice, setNewPrice] = useState("")
  const [newImage, setNewImage] = useState<File | null>(null)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)
  const [addError, setAddError] = useState<string | null>(null)

  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null)
  const [detailProduct, setDetailProduct] = useState<Product | null>(null)
  const [manageProduct, setManageProduct] = useState<Product | null>(null)
  const [viewProfileId, setViewProfileId] = useState<number | null>(null)
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
      // fetch reviews for products to compute average rating (only if enabled)
      if (showReviews) {
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
    const handler = async (e: Event) => {
      if (!showReviews) return
      try {
        const pid = (e as CustomEvent<{ productId: number }>)?.detail?.productId
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
        image: newImage || undefined,
      })

      setNewName("")
      setNewDescription("")
      setNewPrice("")
      setNewImage(null)
      setImagePreview(null)
      await loadProducts()
    } catch {
      setAddError("Failed to add product")
    } finally {
      setAdding(false)
    }
  }

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setNewImage(file)
      const reader = new FileReader()
      reader.onloadend = () => {
        setImagePreview(reader.result as string)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleProductClick = (product: Product) => {
    if (canCreateProduct) {
      setManageProduct(product)
    } else {
      setDetailProduct(product)
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
          <div className="flex flex-col gap-3">
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
            </div>
            <div className="flex flex-col gap-2 md:flex-row md:items-center">
              <div className="flex-1">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Product Image
                </label>
                <Input
                  type="file"
                  accept="image/*"
                  onChange={handleImageChange}
                  className="rounded-xl"
                />
              </div>
              {imagePreview && (
                <div className="relative w-20 h-20 rounded-lg overflow-hidden border border-gray-200">
                  <img
                    src={imagePreview}
                    alt="Preview"
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => {
                      setNewImage(null)
                      setImagePreview(null)
                    }}
                    className="absolute top-1 right-1 bg-red-500 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs hover:bg-red-600"
                  >
                    ×
                  </button>
                </div>
              )}
              <Button type="submit" disabled={adding} className="rounded-full">
                {adding ? "Adding..." : "Add"}
              </Button>
            </div>
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

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {products.map((p) => (
          <Card
            key={p.id}
            onClick={() => handleProductClick(p)}
            className="group relative overflow-hidden border-slate-200/80 shadow-lg transition duration-300 hover:-translate-y-1 hover:shadow-2xl cursor-pointer"
          >
            <span className="pointer-events-none absolute inset-0 bg-gradient-to-br from-emerald-50 via-transparent to-transparent opacity-0 transition duration-300 group-hover:opacity-100" />
            
            {/* Product Image */}
            <div className="relative w-full h-48 bg-gray-100 overflow-hidden">
              {p.imageUrl ? (
                <img
                  src={p.imageUrl}
                  alt={p.name}
                  className="w-full h-full object-cover transition duration-300 group-hover:scale-105"
                />
              ) : (
                <div className="w-full h-full flex items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200">
                  <svg
                    className="w-16 h-16 text-slate-300"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={1.5}
                      d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                    />
                  </svg>
                </div>
              )}
              {!p.available && (
                <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                  <span className="text-white font-semibold px-3 py-1 bg-red-500 rounded-full text-sm">
                    Unavailable
                  </span>
                </div>
              )}
              {p.owner?.id === userId && (
                <div className="absolute top-2 left-2 bg-emerald-500 text-white rounded-full px-3 py-1 text-xs font-semibold shadow-lg">
                  Your listing
                </div>
              )}
              {showReviews && ratingsMap[p.id]?.count > 0 && (
                <div className="absolute top-2 right-2 bg-white/90 backdrop-blur-sm rounded-full px-2 py-1 flex items-center gap-1 text-sm font-medium text-yellow-600 shadow">
                  <span>⭐</span>
                  <span>{ratingsMap[p.id].avg.toFixed(1)}</span>
                  <span className="text-xs text-gray-500">({ratingsMap[p.id].count})</span>
                </div>
              )}
            </div>

            {/* Product Info */}
            <CardContent className="p-4">
              <h3 className="font-semibold text-lg text-slate-900 truncate">{p.name}</h3>
              <p className="text-emerald-600 font-bold text-xl mt-1">
                {p.price.toFixed(2)} € <span className="text-sm font-normal text-slate-500">/ day</span>
              </p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Product Detail Modal - for renters */}
      {detailProduct && (
        <ProductDetailModal
          product={detailProduct}
          userId={userId}
          rating={ratingsMap[detailProduct.id]}
          onClose={() => setDetailProduct(null)}
          onReserve={() => {
            setDetailProduct(null)
            setSelectedProduct(detailProduct)
          }}
          onViewProfile={(ownerId) => {
            setViewProfileId(ownerId)
          }}
        />
      )}

      {/* Booking Modal - for making reservations */}
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

      {/* Product Bookings Modal - for owners */}
      {manageProduct && (
        <ProductBookingsModal
          product={manageProduct}
          userId={userId}
          onClose={() => setManageProduct(null)}
        />
      )}

      {viewProfileId && (
        <UserProfileModal
          userId={viewProfileId}
          onClose={() => setViewProfileId(null)}
        />
      )}
    </div>
  )
}
