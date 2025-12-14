import { SignupForm } from "../components/signup-form"

export default function Page() {
  return (
    <div className="relative min-h-screen w-full overflow-hidden text-slate-50">
      <div className="aurora-blob -left-10 top-12 h-64 w-64 bg-emerald-500/40" />
      <div className="aurora-blob delay-1 right-0 top-48 h-64 w-64 bg-cyan-400/30" />
      <div className="aurora-blob delay-2 -bottom-8 left-28 h-64 w-64 bg-blue-500/25" />

      <header className="relative z-10 flex w-full items-center justify-between px-6 py-6">
        <div className="flex items-center gap-3">
          <span className="inline-flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-emerald-400 via-cyan-400 to-blue-500 shadow-lg shadow-emerald-900/40" />
          <div className="text-left leading-tight">
            <p className="text-xs uppercase tracking-[0.28em] text-emerald-100">BlackTie</p>
          </div>
        </div>
        <button
          onClick={() => (window.location.href = "/login")}
          className="rounded-full border border-white/20 bg-white/10 px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-emerald-900/20 transition hover:-translate-y-0.5 hover:border-white/30 hover:bg-white/15"
        >
          Sign in
        </button>
      </header>

      <div className="relative z-10 flex min-h-[70vh] items-center justify-center px-6 pb-12">
        <div className="fade-up w-full max-w-md">
          <SignupForm className="rounded-3xl border-white/10 bg-white/80 text-slate-900 shadow-2xl backdrop-blur" />
        </div>
      </div>
    </div>
  )
}
