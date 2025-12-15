import { useEffect, useState, useRef } from "react"
import { getProducts, createProduct, getReviewsByProduct, getPortugueseMunicipalities, isValidPortugueseMunicipality, type Product } from "@/lib/api"
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
  const [searchCity, setSearchCity] = useState("")
  const [searchCitySuggestions, setSearchCitySuggestions] = useState<string[]>([])
  const searchCityInputRef = useRef<HTMLDivElement>(null)

  const [newName, setNewName] = useState("")
  const [newDescription, setNewDescription] = useState("")
  const [newPrice, setNewPrice] = useState("")
  const [newDepositAmount, setNewDepositAmount] = useState("")
  const [newSize, setNewSize] = useState("")
  const [newCity, setNewCity] = useState("")
  const [municipalitySuggestions, setMunicipalitySuggestions] = useState<string[]>([])
  const [isValidMunicipality, setIsValidMunicipality] = useState(false)
  const municipalityInputRef = useRef<HTMLDivElement>(null)
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

  // Municipality autocomplete with validation (for add product form)
  useEffect(() => {
    const fetchMunicipalities = async () => {
      if (newCity.length >= 2) {
        const municipalities = await getPortugueseMunicipalities(newCity)
        setMunicipalitySuggestions(municipalities)
        // Validate if current value matches any valid municipality (accent-insensitive)
        const isValid = await isValidPortugueseMunicipality(newCity)
        setIsValidMunicipality(isValid)
      } else {
        setMunicipalitySuggestions([])
        setIsValidMunicipality(false)
      }
    }

    const timeoutId = setTimeout(fetchMunicipalities, 300)
    return () => clearTimeout(timeoutId)
  }, [newCity])

  // Municipality autocomplete for search form
  useEffect(() => {
    const fetchSearchMunicipalities = async () => {
      if (searchCity.length >= 2) {
        const municipalities = await getPortugueseMunicipalities(searchCity)
        setSearchCitySuggestions(municipalities)
      } else {
        setSearchCitySuggestions([])
      }
    }

    const timeoutId = setTimeout(fetchSearchMunicipalities, 300)
    return () => clearTimeout(timeoutId)
  }, [searchCity])

  // Close suggestions when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (municipalityInputRef.current && !municipalityInputRef.current.contains(event.target as Node)) {
        setMunicipalitySuggestions([])
      }
      if (searchCityInputRef.current && !searchCityInputRef.current.contains(event.target as Node)) {
        setSearchCitySuggestions([])
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
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

    if (newCity && !isValidMunicipality) {
      setAddError("Please select a valid municipality from the suggestions")
      return
    }

    const depositNumber = newDepositAmount.trim() ? Number(newDepositAmount) : undefined
    if (depositNumber !== undefined && (Number.isNaN(depositNumber) || depositNumber < 0)) {
      setAddError("Deposit amount must be a positive number")
      return
    }

    try {
      setAdding(true)
      await createProduct(userId, {
        name: newName.trim(),
        description: newDescription.trim(),
        price: priceNumber,
        depositAmount: depositNumber,
        city: newCity.trim() || undefined,
        size: newSize.trim() || undefined,
        image: newImage || undefined,
      })

      setNewName("")
      setNewDescription("")
      setNewPrice("")
      setNewDepositAmount("")
      setNewSize("")
      setNewCity("")
      setIsValidMunicipality(false)
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

  // Filter products by municipality (client-side filtering)
  const filteredProducts = searchCity.trim()
    ? products.filter((p) => {
      if (!p.city) return false
      const normalizedSearchCity = searchCity.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      const normalizedProductCity = p.city.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      return normalizedProductCity.includes(normalizedSearchCity)
    })
    : products

  return (
    <div className="mt-4 w-full space-y-6">
      <form
        onSubmit={handleSubmit}
        className="flex flex-wrap items-end gap-3 rounded-2xl border border-slate-200/70 bg-white/80 p-4 shadow-lg backdrop-blur md:p-5 relative z-20"
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

        <div ref={searchCityInputRef} className="flex min-w-[180px] flex-col relative">
          <label className="text-sm font-medium" htmlFor="searchCity">
            Municipality
          </label>
          <div className="relative">
            <Input
              id="searchCity"
              value={searchCity}
              onChange={(e) => setSearchCity(e.target.value)}
              placeholder="All municipalities"
              autoComplete="off"
            />
            {searchCity && (
              <button
                type="button"
                onClick={() => setSearchCity("")}
                className="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-sm"
                title="Clear municipality filter"
              >
                ✕
              </button>
            )}
          </div>
          {searchCitySuggestions.length > 0 && (
            <div className="absolute top-full left-0 right-0 z-[100] mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
              {searchCitySuggestions.map((municipality, index) => (
                <button
                  key={index}
                  type="button"
                  onClick={() => {
                    setSearchCity(municipality)
                    setSearchCitySuggestions([])
                  }}
                  className="w-full text-left px-4 py-2.5 hover:bg-blue-50 text-sm transition-colors first:rounded-t-lg last:rounded-b-lg border-b last:border-b-0"
                >
                  {municipality}
                </button>
              ))}
            </div>
          )}
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
              <Input
                type="number"
                min={0}
                step={1}
                placeholder="Deposit (optional)"
                value={newDepositAmount}
                onChange={(e) => setNewDepositAmount(e.target.value)}
                className="w-36 rounded-xl"
                title="Optional deposit amount for this product"
              />
              <Input
                placeholder="Size (e.g., S, M, L, XL)"
                value={newSize}
                onChange={(e) => setNewSize(e.target.value)}
                className="w-32 rounded-xl"
                title="Product size (optional)"
              />
            </div>
            <div className="flex flex-col gap-2">
              <div ref={municipalityInputRef} className="relative">
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Municipality {newCity && (
                    isValidMunicipality
                      ? <span className="text-green-600 ml-2">✓ Valid</span>
                      : <span className="text-amber-600 ml-2">⚠ Please select from list</span>
                  )}
                </label>
                <Input
                  placeholder="Type to search municipality..."
                  value={newCity}
                  onChange={(e) => setNewCity(e.target.value)}
                  className={`rounded-xl ${newCity && !isValidMunicipality ? 'border-amber-400 focus:border-amber-500' : ''}`}
                  title="Municipality name"
                  autoComplete="off"
                />
                {municipalitySuggestions.length > 0 && (
                  <div className="absolute z-[100] w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                    {municipalitySuggestions.map((municipality, index) => (
                      <button
                        key={index}
                        type="button"
                        onClick={() => {
                          setNewCity(municipality)
                          setMunicipalitySuggestions([])
                          setIsValidMunicipality(true)
                        }}
                        className="w-full text-left px-4 py-2.5 hover:bg-blue-50 text-sm transition-colors first:rounded-t-lg last:rounded-b-lg border-b last:border-b-0"
                      >
                        {municipality}
                      </button>
                    ))}
                  </div>
                )}
                {newCity.length >= 2 && municipalitySuggestions.length === 0 && (
                  <p className="text-sm text-gray-500 mt-1">No municipalities found. Keep typing...</p>
                )}
              </div>
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

      {filteredProducts.length === 0 && !loading && (
        <div className="text-sm text-gray-500 text-center">
          {products.length === 0 ? "No products found." : `No products found in "${searchCity}".`}
        </div>
      )}

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {filteredProducts.map((p) => (
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
              {p.city && (
                <p className="text-sm text-slate-500 mt-1 flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  {p.city}
                </p>
              )}
              {p.depositAmount && p.depositAmount > 0 && (
                <p className="text-sm text-amber-700 mt-1 flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  Deposit: {p.depositAmount.toFixed(2)} €
                </p>
              )}
              {p.size && (
                <p className="text-sm text-slate-600 mt-1 flex items-center gap-1">
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 8V4m0 0h4M4 4l5 5m11-1V4m0 0h-4m4 0l-5 5M4 16v4m0 0h4m-4 0l5-5m11 5l-5-5m5 5v-4m0 4h-4" />
                  </svg>
                  Size: {p.size}
                </p>
              )}
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
          onProductDeleted={() => {
            setManageProduct(null)
            void loadProducts()
          }}
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
