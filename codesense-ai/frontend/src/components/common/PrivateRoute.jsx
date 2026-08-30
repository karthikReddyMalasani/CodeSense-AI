import React from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function PrivateRoute() {
  const { token, loading } = useAuth();
  const location = useLocation();
  const hasOAuthSuccessCallback = window.location.hash.includes('access_token=') ||
    window.location.hash.includes('code=') ||
    window.location.search.includes('code=');

  if (loading) return <div className="loading-center"><div className="spinner" /></div>;

  if (!token) {
    if (hasOAuthSuccessCallback) {
      return <div className="loading-center"><div className="spinner" /></div>;
    }

    const hasError = location.search.includes('error=') || window.location.hash.includes('error=');
    const search = hasError ? (location.search || `?${window.location.hash.replace('#', '')}`) : '';
    return <Navigate to={`/login${search}`} replace />;
  }

  return <Outlet />;
}

