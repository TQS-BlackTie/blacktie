import { BrowserRouter, Routes, Route } from 'react-router-dom'
import HomePage from './app/home'
import SignUpPage from './app/signup'
import SignInPage from './app/signin'
import RoleSetupPage from './app/role-setup'
import ProfilePage from './app/profile'
import MyBookingsPage from './app/my-bookings'
import BookingHistoryPage from './app/booking-history'
import OwnerBookingsPage from './app/owner-bookings'
import PendingApprovalsPage from './app/pending-approvals'
import MyReputationPage from './app/my-reputation'
import AdminDashboardPage from './app/admin-dashboard'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/register" element={<SignUpPage />} />
        <Route path="/login" element={<SignInPage />} />
        <Route path="/signin" element={<SignInPage />} />
        <Route path="/role-setup" element={<RoleSetupPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/my-bookings" element={<MyBookingsPage />} />
        <Route path="/active-bookings" element={<MyBookingsPage />} />
        <Route path="/history" element={<BookingHistoryPage />} />
        <Route path="/booking-history" element={<BookingHistoryPage />} />
        <Route path="/owner-bookings" element={<OwnerBookingsPage />} />
        <Route path="/pending-approvals" element={<PendingApprovalsPage />} />
        <Route path="/my-reputation" element={<MyReputationPage />} />
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin-dashboard" element={<AdminDashboardPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
