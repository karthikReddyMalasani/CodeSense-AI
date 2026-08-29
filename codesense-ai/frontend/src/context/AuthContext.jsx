import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../services/api';
import { supabase } from '../services/supabase';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const exchangeInProgress = useRef(false);
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;

    const loadProfile = async () => {
      const { data: { session } } = await supabase.auth.getSession();
      if (session?.user && !localStorage.getItem('token')) {
        try {
          await exchangeSession(session);
          if (mounted) navigate('/dashboard', { replace: true });
        } catch (error) {
          if (mounted) setLoading(false);
          console.error('Unable to create the CodeSense session:', error);
        }
        return;
      }
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
        exchangeSession(session, undefined, provider === 'google' || provider === 'github').then(() => {
          if (mounted) navigate('/dashboard', { replace: true });
        }).catch((error) => {
          if (mounted) setLoading(false);
          console.error('Unable to create the CodeSense session:', error);
        });
      }
    });

    return () => {
      mounted = false;
      subscription.unsubscribe();
    };
  }, [token, navigate]);

  // Supabase verifies credentials; the exchange returns the app API token.
  const exchangeSession = async (session, name, requireVerifiedEmail = false) => {
    if (exchangeInProgress.current) return user;
    if (!session?.user?.email) throw new Error('Supabase did not return an email for this account.');
    if (requireVerifiedEmail && !session.user.email_confirmed_at && !session.user.confirmed_at) {
      await supabase.auth.signOut();
      throw new Error('Your Google or GitHub email could not be verified. Please use a verified provider account.');
    }
    exchangeInProgress.current = true;
    try {
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
    } finally {
      exchangeInProgress.current = false;
    }
  };

  const login = async (email, password) => {
    try {
      const { data, error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
      if (error) throw error;
      return exchangeSession(data.session);
    } catch (supabaseError) {
      // If Supabase login fails, try verifying legacy account
      console.warn('Supabase login failed, attempting legacy account verification:', supabaseError.message);
      try {
        const legacyResponse = await authApi.legacyLogin({
          email: email.trim(),
          password
        });
        
        const legacyData = legacyResponse.data.data;
        
        // Legacy account found - now create Supabase account with same credentials
        const { data: signUpData, error: signUpError } = await supabase.auth.signUp({
          email: legacyData.email,
          password,
          options: {
            data: { name: legacyData.name },
            autoConfirm: true // Auto-confirm since they're an existing user
          }
        });
        
        if (signUpError) {
          // If account already exists in Supabase, try signing in
          if (signUpError.message?.includes('already registered')) {
            const { data, error } = await supabase.auth.signInWithPassword({ 
              email: legacyData.email, 
              password 
            });
            if (error) throw error;
            return exchangeSession(data.session);
          }
          throw signUpError;
        }
        
        if (!signUpData.session) {
          throw new Error('Account created but session not established. Please log in again.');
        }
        
        return exchangeSession(signUpData.session, legacyData.name);
      } catch (legacyError) {
        // If legacy verification also fails, throw the original Supabase error
        if (legacyError.response?.status === 404 || legacyError.message?.includes('not found')) {
          throw new Error('Invalid credentials. Please check your email and password.');
        }
        throw legacyError;
      }
    }
  };

  const register = async (name, email, password) => {
    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
      options: {
        data: { name: name.trim() },
        emailRedirectTo: `${window.location.origin}/dashboard`
      }
    });
    if (error) throw error;
    if (!data.session) {
      throw new Error('Account created. Verify your email using the link we sent, then return here to sign in.');
    }
    if (!data.user?.email_confirmed_at && !data.user?.confirmed_at) {
      await supabase.auth.signOut();
      throw new Error('Account created. Verify your email using the link we sent before continuing.');
    }
    return exchangeSession(data.session, name);
  };

  const sendMagicLink = async (email) => {
    const { error } = await supabase.auth.signInWithOtp({
      email: email.trim(),
      options: { emailRedirectTo: `${window.location.origin}/dashboard` }
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
