import { useEffect, useState } from 'react'
import HomePage from './app/home'
import SignUpPage from './app/signup'
import SignInPage from './app/signin'
import RoleSetupPage from './app/role-setup'
import ProfilePage from './app/profile'
import MyBookingsPage from './app/my-bookings'
import OwnerBookingsPage from './app/owner-bookings'
import PendingApprovalsPage from './app/pending-approvals'
import './App.css'

function App() {
  const [currentPath, setCurrentPath] = useState(window.location.pathname)

  useEffect(() => {
    const handleLocationChange = () => {
      setCurrentPath(window.location.pathname)
    }

    window.addEventListener('popstate', handleLocationChange)
    return () => window.removeEventListener('popstate', handleLocationChange)
  }, [])

  if (currentPath === '/signup' || currentPath === '/register') {
    return <SignUpPage />
  }

  if (currentPath === '/login' || currentPath === '/signin') {
    return <SignInPage />
  }

  if (currentPath === '/role-setup') {
    return <RoleSetupPage />
  }

  if (currentPath === '/profile') {
    return <ProfilePage />
  }

  if (currentPath === '/my-bookings' || currentPath === '/history' || currentPath === '/active-bookings') {
    return <MyBookingsPage />
  }

  if (currentPath === '/owner-bookings') {
    return <OwnerBookingsPage />
  }

  if (currentPath === '/pending-approvals') {
    return <PendingApprovalsPage />
  }

  if (currentPath === '/my-bookings') {
    return <MyBookingsPage />
  }

  return <HomePage />
}

export default App
