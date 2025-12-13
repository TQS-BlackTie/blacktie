import { useEffect, useState } from "react"
import { Button } from "./ui/button"

type NavbarProps = {
  userName?: string
  userRole?: string
  onLogout?: () => void
  notificationBell?: React.ReactNode
}

export function Navbar({ userName, userRole, onLogout, notificationBell }: NavbarProps) {
  const [activePath, setActivePath] = useState<string>("")

  useEffect(() => {
    if (typeof window === "undefined") return

    const updatePath = () => setActivePath(window.location.pathname)
    updatePath()
    window.addEventListener("popstate", updatePath)
    return () => window.removeEventListener("popstate", updatePath)
  }, [])

  const allNavItems = [
    { label: "Catalog", href: "/" },
    { label: "My Bookings", href: "/my-bookings", roleRequired: "renter" },
    { label: "Profile", href: "/profile" },
    { label: "Role Setup", href: "/role-setup" },
  ]

  const navItems = allNavItems.filter(
    (item) => !item.roleRequired || item.roleRequired === userRole
  )

  const goTo = (href: string) => {
    if (typeof window === "undefined") return
    window.location.href = href
  }

  return (
    <header className="sticky top-0 z-30 border-b border-white/10 bg-slate-950/70 backdrop-blur">
      <div className="flex w-full items-center gap-4 px-6 py-4">
        <button
          className="group flex items-center gap-3"
          onClick={() => goTo("/")}
        >
          <span className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-400 via-cyan-400 to-blue-500 shadow-lg shadow-emerald-900/40 transition-transform duration-300 group-hover:scale-105" />
          <div className="text-left leading-tight">
            <p className="text-xs uppercase tracking-[0.28em] text-emerald-100">BlackTie</p>
          </div>
        </button>

        <nav className="ml-6 hidden items-center gap-2 md:flex">
          {navItems.map((item) => {
            const isActive = activePath === item.href
            return (
              <button
                key={item.href}
                onClick={() => goTo(item.href)}
                className={`rounded-full border px-4 py-2 text-sm font-medium transition duration-200 ${
                  isActive
                    ? "border-white/30 bg-white/15 text-white shadow-lg shadow-emerald-900/20"
                    : "border-white/5 text-slate-200 hover:border-white/15 hover:bg-white/10"
                }`}
              >
                {item.label}
              </button>
            )
          })}
        </nav>

        <div className="ml-auto flex items-center gap-3">
          {notificationBell}
          <div className="hidden text-right leading-tight sm:flex sm:flex-col">
            <span className="text-sm font-semibold text-white">
              {userName || "Guest"}
            </span>
            {userRole && (
              <span className="text-xs capitalize text-emerald-100/80">
                {userRole}
              </span>
            )}
          </div>
          {onLogout && (
            <Button
              variant="secondary"
              className="rounded-full bg-white text-slate-900 shadow-lg shadow-emerald-900/10 transition hover:-translate-y-0.5 hover:bg-emerald-100"
              onClick={onLogout}
            >
              Logout
            </Button>
          )}
        </div>
      </div>
    </header>
  )
}
