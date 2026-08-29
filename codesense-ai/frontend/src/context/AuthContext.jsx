import React, { createContext, useContext, useEffect, useState } from 'react';
import { authApi } from '../services/api';
import { supabase } from '../services/supabase';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    const loadProfile = async () => {
      if (!token) {
        if (mounted) setLoading(false);
        return;
      }
      try {
        const res = await authApi.getMe();
        if (mounted) setUser(res.data.data);
      } catch {
        localStorage.removeItem('token');
        if (mounted) {
          setToken(null);
          setUser(null);
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    loadProfile();

    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'SIGNED_OUT' && mounted) {
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
      }
      if ((event === 'SIGNED_IN' || event === 'INITIAL_SESSION') && session?.user && !localStorage.getItem('token')) {
        setLoading(true);
        const provider = session.user.app_metadata?.provider;
        exchangeSession(session, undefined, provider === 'google' || provider === 'github').catch((error) => {
          if (mounted) setLoading(false);
          console.error('Unable to create the CodeSense session:', error);
        });
      }
    });

    return () => {
      mounted = false;
      subscription.unsubscribe();
    };
  }, [token]);

  // Supabase verifies credentials; the exchange returns the app API token.
  const exchangeSession = async (session, name, requireVerifiedEmail = false) => {
    if (!session?.user?.email) throw new Error('Supabase did not return an email for this account.');
    if (requireVerifiedEmail && !session.user.email_confirmed_at && !session.user.confirmed_at) {
      await supabase.auth.signOut();
      throw new Error('Your Google or GitHub email could not be verified. Please use a verified provider account.');
    }
    const res = await authApi.socialLogin({
      provider: 'supabase',
      email: session.user.email,
      name: name || session.user.user_metadata?.name || session.user.email.split('@')[0]
    });
    const { token: newToken, ...userData } = res.data.data;
    localStorage.setItem('token', newToken);
    setToken(newToken);
    setUser(userData);
    return userData;
  };

  const login = async (email, password) => {
    const { data, error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
    if (error) throw error;
    return exchangeSession(data.session);
  };

  const register = async (name, email, password) => {
    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
      options: { data: { name: name.trim() } }
    });
    if (error) throw error;
    if (!data.session) throw new Error('Check your email to confirm your account, then sign in.');
    return exchangeSession(data.session, name);
  };

  const sendMagicLink = async (email) => {
    const { error } = await supabase.auth.signInWithOtp({
      email: email.trim(),
      options: { emailRedirectTo: `${window.location.origin}/login` }
    });
    if (error) throw error;
  };

  const socialLogin = async (provider) => {
    const { error } = await supabase.auth.signInWithOAuth({
      provider: provider.toLowerCase(),
      options: { redirectTo: window.location.origin }
    });
    if (error) throw error;
  };

  const logout = () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    supabase.auth.signOut();
  };

  return (
    <AuthContext.Provider value={{ user, token, loading, login, register, sendMagicLink, socialLogin, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
