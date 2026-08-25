import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

export default function PrivateRoute() {
  const { token, loading } = useAuth();
  if (loading) return <div className="loading-center"><div className="spinner" /></div>;
  return token ? <Outlet /> : <Navigate to="/login" replace />;
}
