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

    const isOAuth = session.user.app_metadata?.provider === 'github' ||
      session.user.app_metadata?.provider === 'google' ||
      session.user.app_metadata?.provider === 'gitlab';

    if (requireVerifiedEmail && !isOAuth && !session.user.email_confirmed_at && !session.user.confirmed_at) {
      await supabase.auth.signOut();
      throw new Error('Your email could not be verified. Please check your inbox for the verification email.');
    }

    exchangeInProgress.current = true;
    try {
      const displayName = name ||
        session.user.user_metadata?.name ||
        session.user.user_metadata?.full_name ||
        session.user.user_metadata?.preferred_username ||
        session.user.email.split('@')[0];

      const res = await authApi.socialLogin({
        provider: 'supabase',
        email: session.user.email,
        name: displayName
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
    // Try direct Spring Boot backend login first
    try {
      const res = await authApi.login({ email: email.trim(), password });
      const { token: newToken, ...userData } = res.data.data;
      localStorage.setItem('token', newToken);
      setToken(newToken);
      setUser(userData);
      return userData;
    } catch (backendError) {
      console.warn('Backend login error:', backendError);
      // If backend returns a clear error message (e.g. Bad credentials), throw it directly
      if (backendError.response?.data?.message) {
        throw new Error(backendError.response.data.message);
      }
    }

    // Fallback to Supabase login / legacy verification
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
            emailRedirectTo: `${window.location.origin}/dashboard`
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

        // If signup was successful but no session yet, sign in immediately
        if (signUpData.session) {
          return exchangeSession(signUpData.session, legacyData.name);
        }

        // Account created but no auto session - sign in manually
        console.log('Account created, signing in manually...');
        const { data: signInData, error: signInError } = await supabase.auth.signInWithPassword({
          email: legacyData.email,
          password
        });

        if (signInError) {
          // If still can't sign in, user needs to verify email
          console.warn('Sign in after signup failed, user may need email verification');
          throw new Error('Account created. Please check your email for verification link and try logging in again.');
        }

        if (!signInData.session) {
          throw new Error('Account created but session could not be established. Please try logging in again.');
        }

        return exchangeSession(signInData.session, legacyData.name);
      } catch (legacyError) {
        // If legacy verification also fails, throw the original Supabase error
        if (legacyError.response?.status === 404 || legacyError.message?.includes('not found')) {
          throw new Error('Invalid credentials. Please check your email and password.');
        }
        throw legacyError;
      }
    }
  };

  // signUpWithVerification: sends Supabase verification email.
  // Backend account is created automatically when user clicks the email link
  // (handled via onAuthStateChange -> exchangeSession -> authApi.socialLogin)
  const signUpWithVerification = async (name, email, password) => {
    const { data, error } = await supabase.auth.signUp({
      email: email.trim(),
      password,
      options: {
        data: { name: name.trim() },
        emailRedirectTo: `${window.location.origin}/dashboard`
      }
    });
    if (error) throw error;

    // If Supabase returns a session immediately (email confirmations disabled in Supabase dashboard)
    // still force email verification by signing out and asking user to check email
    if (data.session && !data.user?.email_confirmed_at) {
      await supabase.auth.signOut();
    }

    // Return info for UI — user must click the email link
    return { email: email.trim(), name: name.trim() };
  };

  const register = async (name, email, password) => {
    // Register directly in local backend database
    try {
      const res = await authApi.register({
        name: name.trim(),
        email: email.trim(),
        password
      });
      const { token: newToken, ...userData } = res.data.data;
      localStorage.setItem('token', newToken);
      setToken(newToken);
      setUser(userData);
      return userData;
    } catch (backendError) {
      console.warn('Backend register error:', backendError);
      if (backendError.response?.data?.message) {
        throw new Error(backendError.response.data.message);
      }
      throw backendError;
    }
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
      options: { redirectTo: `${window.location.origin}/dashboard` }
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
    <AuthContext.Provider value={{ user, token, loading, login, register, signUpWithVerification, sendMagicLink, socialLogin, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
