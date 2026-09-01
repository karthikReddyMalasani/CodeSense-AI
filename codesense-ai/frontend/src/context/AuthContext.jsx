import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../services/api';
import { supabase } from '../services/supabase';

const AuthContext = createContext(null);
const AUTH_STATES = {
  INITIALIZING: 'AUTH_INITIALIZING',
  AUTHENTICATING: 'AUTHENTICATING',
  AUTHENTICATED: 'AUTHENTICATED',
  UNAUTHENTICATED: 'UNAUTHENTICATED',
  AUTH_ERROR: 'AUTH_ERROR'
};

const devLog = (...args) => {
  if (import.meta.env.DEV) {
    console.debug('[AUTH]', ...args);
  }
};

export function AuthProvider({ children }) {
  const navigate = useNavigate();
  const exchangeInProgress = useRef(false);
  const initializedRef = useRef(false);

  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [authState, setAuthState] = useState(AUTH_STATES.INITIALIZING);
  const [authError, setAuthError] = useState('');

  const loading = authState === AUTH_STATES.INITIALIZING || authState === AUTH_STATES.AUTHENTICATING;

  useEffect(() => {
    if (initializedRef.current) return;
    initializedRef.current = true;

    let mounted = true;

    const isOAuthSuccessCallback = typeof window !== 'undefined' &&
      (window.location.hash.includes('access_token=') ||
        window.location.hash.includes('code=') ||
        window.location.search.includes('code='));

    const isOAuthErrorCallback = typeof window !== 'undefined' &&
      (window.location.hash.includes('error=') ||
        window.location.search.includes('error='));

    const restoreSession = async () => {
      const storedToken = localStorage.getItem('token');
      devLog('auth:init:start', { hasStoredToken: Boolean(storedToken) });

      if (!storedToken) {
        if (mounted) setAuthState(AUTH_STATES.UNAUTHENTICATED);
        return;
      }

      try {
        const res = await authApi.getMe();
        if (mounted) {
          setUser(res.data?.data || null);
          setToken(storedToken);
          setAuthState(AUTH_STATES.AUTHENTICATED);
          devLog('auth:init:authenticated');
        }
      } catch (error) {
        localStorage.removeItem('token');
        if (mounted) {
          setToken(null);
          setUser(null);
          setAuthState(AUTH_STATES.UNAUTHENTICATED);
          devLog('auth:init:clear-token');
        }
      }
    };

    if (!token && isOAuthSuccessCallback && !isOAuthErrorCallback) {
      supabase.auth.getSession()
        .then(({ data: { session } }) => {
          if (!session?.user) throw new Error('OAuth callback did not include a user session.');
          return exchangeSession(session);
        })
        .then(() => {
          if (mounted) {
            setAuthState(AUTH_STATES.AUTHENTICATED);
            navigate('/dashboard', { replace: true });
          }
        })
        .catch((error) => {
          if (mounted) {
            setAuthState(AUTH_STATES.UNAUTHENTICATED);
          }
          console.error('Unable to create the CodeSense session:', error);
        });
      return () => {
        mounted = false;
      };
    }

    restoreSession();

    const { data: { subscription } } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === 'SIGNED_OUT' && mounted) {
        localStorage.removeItem('token');
        setToken(null);
        setUser(null);
        setAuthState(AUTH_STATES.UNAUTHENTICATED);
        devLog('auth:supabase:sign-out');
      }

      if ((event === 'SIGNED_IN' || event === 'INITIAL_SESSION' || event === 'TOKEN_REFRESHED') && session?.user && !localStorage.getItem('token')) {
        setAuthState(AUTH_STATES.AUTHENTICATING);
        exchangeSession(session)
          .then(() => {
            if (mounted) {
              setAuthState(AUTH_STATES.AUTHENTICATED);
              navigate('/dashboard', { replace: true });
            }
          })
          .catch((error) => {
            if (mounted) {
              setAuthState(AUTH_STATES.UNAUTHENTICATED);
            }
            console.error('Unable to create the CodeSense session:', error);
          });
      }
    });

    return () => {
      mounted = false;
      subscription.unsubscribe();
    };
  }, [navigate, token]);

  const applyAuthenticatedSession = (newToken, nextUser) => {
    localStorage.setItem('token', newToken);
    setToken(newToken);
    setUser(nextUser);
    setAuthState(AUTH_STATES.AUTHENTICATED);
    devLog('auth:session:applied');
  };

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
        provider: session.user.app_metadata?.provider || 'google',
        email: session.user.email,
        name: displayName,
        accessToken: session.access_token
      });
      const { token: newToken, ...userData } = res.data.data;
      applyAuthenticatedSession(newToken, userData);
      return userData;
    } finally {
      exchangeInProgress.current = false;
    }
  };

  const login = async (email, password) => {
    setAuthError('');
    setAuthState(AUTH_STATES.AUTHENTICATING);
    devLog('auth:login:start');
    try {
      const res = await authApi.login({ email: email.trim(), password });
      const { token: newToken, ...userData } = res.data.data;
      applyAuthenticatedSession(newToken, userData);
      devLog('auth:login:success');
      return userData;
    } catch (backendError) {
      setAuthState(AUTH_STATES.UNAUTHENTICATED);
      if (!backendError.response) {
        const error = new Error("We couldn't connect to the authentication server. Please try again.");
        setAuthError(error.message);
        throw error;
      }
      const status = backendError.response.status;
      const message = status === 401
        ? 'Invalid email or password.'
        : status === 400
          ? 'Please check the information you entered.'
          : backendError.response.data?.message || 'Unable to sign in. Please try again.';
      setAuthError(message);
      throw new Error(message);
    }
  };

  const signUpWithVerification = async (name, email, password) => {
    return register(email, password);
  };

  const register = async (email, password) => {
    setAuthError('');
    setAuthState(AUTH_STATES.AUTHENTICATING);
    devLog('auth:register:start');
    try {
      const res = await authApi.register({
        email: email.trim().toLowerCase(),
        password
      });
      const { token: newToken, ...userData } = res.data.data;
      applyAuthenticatedSession(newToken, userData);
      devLog('auth:register:success');
      return userData;
    } catch (backendError) {
      setAuthState(AUTH_STATES.UNAUTHENTICATED);
      if (!backendError.response) {
        const error = new Error("We couldn't connect to the authentication server. Please try again.");
        setAuthError(error.message);
        throw error;
      }
      const status = backendError.response.status;
      const message = status === 409
        ? 'An account with this email already exists.'
        : status === 400
          ? 'Please check the information you entered.'
          : backendError.response.data?.message || 'Unable to create your account. Please try again.';
      setAuthError(message);
      throw new Error(message);
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

  const logout = async () => {
    localStorage.removeItem('token');
    setToken(null);
    setUser(null);
    setAuthError('');
    setAuthState(AUTH_STATES.UNAUTHENTICATED);
    devLog('auth:logout');
    try {
      await supabase.auth.signOut();
    } catch (error) {
      console.warn('Supabase signOut failed during logout.', error);
    }
  };

  return (
    <AuthContext.Provider value={{
      user,
      token,
      loading,
      authError,
      authState,
      login,
      register,
      signUpWithVerification,
      sendMagicLink,
      socialLogin,
      logout
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
}
