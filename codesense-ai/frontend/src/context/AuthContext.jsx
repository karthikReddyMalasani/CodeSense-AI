import React, { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      if (token.startsWith('mock-dev-token-')) {
        // Try to reach the backend — if it's up, the mock token is invalid, force re-login
        authApi.getMe()
          .then(res => {
            // Shouldn't happen with a mock token, but handle gracefully
            setUser(res.data.data);
            setLoading(false);
          })
          .catch((err) => {
            if (!err.response) {
              // Backend truly offline — keep mock token/user
              setUser({ id: 'mock-user-1', name: 'Developer User', email: 'dev@codesense.ai', role: 'USER' });
            } else {
              // Backend is online but rejected mock token — clear it so user must log in again
              console.warn('Stale mock token detected while backend is online. Clearing session.');
              localStorage.removeItem('token');
              setToken(null);
            }
            setLoading(false);
          });
        return;
      }
      authApi.getMe()
        .then(res => setUser(res.data.data))
        .catch(() => { localStorage.removeItem('token'); setToken(null); })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  const login = async (email, password) => {
    try {
      const res = await authApi.login({ email, password });
      const { token: newToken, ...userData } = res.data.data;
      localStorage.setItem('token', newToken);
      setToken(newToken);
      setUser(userData);
      return userData;
    } catch (err) {
      if (!err.response) {
        throw new Error('Unable to connect to backend server at http://localhost:8080. Please start the backend service to save user credentials.');
      }
      throw err;
    }
  };

  const register = async (name, email, password) => {
    try {
      const res = await authApi.register({ name, email, password });
      const { token: newToken, ...userData } = res.data.data;
      localStorage.setItem('token', newToken);
      setToken(newToken);
      setUser(userData);
      return userData;
    } catch (err) {
      if (!err.response) {
        throw new Error('Unable to connect to backend server at http://localhost:8080. Please start the backend service to save user credentials.');
      }
      throw err;
    }
  };

  const socialLogin = async (provider, email, name) => {
    try {
      const res = await authApi.socialLogin({ provider, email, name });
      const { token: newToken, ...userData } = res.data.data;
      localStorage.setItem('token', newToken);
      setToken(newToken);
      setUser(userData);
      return userData;
    } catch (err) {
      if (!err.response) {
        throw new Error('Unable to connect to backend server at http://localhost:8080. Please start the backend service.');
      }
      throw err;
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, socialLogin, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
