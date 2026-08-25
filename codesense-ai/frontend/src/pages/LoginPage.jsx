import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import '../styles/LoginPage.css';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [loading, setLoading] = useState(false);

  const validateForm = () => {
    const newErrors = {};
    if (!form.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(form.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!form.password) {
      newErrors.password = 'Password is required';
    } else if (form.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setGlobalError('');

    if (!validateForm()) return;

    setLoading(true);
    try {
      await login(form.email, form.password);
      navigate('/dashboard');
    } catch (err) {
      const data = err.response?.data;
      const msg = data?.fieldErrors?.[0]?.message || data?.message || err.message || 'Invalid credentials. Please try again.';
      setGlobalError(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleSocialLogin = async (provider) => {
    setGlobalError('');
    setLoading(true);
    try {
      const demoEmail = form.email.trim() || `${provider.toLowerCase()}_developer@codesense.ai`;
      const demoName = `${provider} Developer`;
      await socialLogin(provider.toLowerCase(), demoEmail, demoName);
      navigate('/dashboard');
    } catch (err) {
      const data = err.response?.data;
      const msg = data?.message || err.message || `${provider} authentication failed. Please try again.`;
      setGlobalError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="cs-login-viewport">
      <div className="cs-login-card-container">

        {/* LEFT COLUMN — BRANDING PANEL */}
        <div className="cs-login-branding">
          <div className="cs-branding-bg-grid" />
          <div className="cs-branding-glow" />

          {/* Top Logo */}
          <div className="cs-branding-header">
            <div className="cs-brand-logo-badge">
              <div className="cs-logo-icon-hexagon">&lt;/&gt;</div>
              <div className="cs-logo-text">
                CodeSense <span>AI</span>
              </div>
            </div>
          </div>

          {/* Main Headline & Subtext */}
          <div className="cs-branding-content">
            <h1 className="cs-branding-headline">
              Smarter Code.<br />
              Better <span className="highlight-blue">Together.</span>
            </h1>
            <p className="cs-branding-subtext">
              Your AI-powered coding companion for understanding, generating, and improving code effortlessly.
            </p>
          </div>

          {/* 3D Code IDE Visual Graphic */}
          <div className="cs-branding-visual">
            <div className="cs-code-window-card">
              <div className="cs-window-dots">
                <span className="cs-dot red" />
                <span className="cs-dot yellow" />
                <span className="cs-dot green" />
              </div>
              <div className="cs-code-lines">
                <div className="cs-code-line">
                  <span className="cs-line-num">1</span>
                  <div className="cs-line-bar cs-bar-purple" />
                  <div className="cs-line-bar cs-bar-blue" />
                </div>
                <div className="cs-code-line">
                  <span className="cs-line-num">2</span>
                  <div className="cs-line-bar cs-bar-cyan" />
                </div>
                <div className="cs-code-line">
                  <span className="cs-line-num">3</span>
                  <div className="cs-line-bar cs-bar-green" />
                  <div className="cs-line-bar cs-bar-pink" />
                </div>
                <div className="cs-code-line">
                  <span className="cs-line-num">4</span>
                  <div className="cs-line-bar cs-bar-blue" />
                </div>
              </div>
            </div>

            {/* Floating 3D Badges */}
            <div className="cs-floating-badge cs-badge-curly">&#123; &#125;</div>
            <div className="cs-floating-badge cs-badge-code">&lt;/&gt;</div>
            <div className="cs-floating-badge cs-badge-ai">AI</div>
          </div>
        </div>

        {/* RIGHT COLUMN — LOGIN CARD */}
        <div className="cs-login-form-panel">
          <div className="cs-login-form-card">

            {/* Header */}
            <div className="cs-form-header">
              <h2 className="cs-form-title">Welcome Back</h2>
              <p className="cs-form-subtitle">Sign in to continue to CodeSense AI</p>
            </div>

            {/* Global Error Banner */}
            {globalError && (
              <div className="cs-global-alert" style={{ marginBottom: '18px' }}>
                {globalError}
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit} className="cs-auth-form" noValidate>

              {/* Email Field */}
              <div className="cs-form-field">
                <label className="cs-field-label" htmlFor="email-input">Email</label>
                <div className={`cs-input-box ${errors.email ? 'error' : ''}`}>
                  <Mail className="cs-input-icon-left" />
                  <input
                    id="email-input"
                    type="email"
                    className="cs-input-control"
                    placeholder="Enter your email"
                    value={form.email}
                    onChange={(e) => {
                      setForm({ ...form, email: e.target.value });
                      if (errors.email) setErrors({ ...errors, email: null });
                    }}
                  />
                </div>
                {errors.email && <div className="cs-field-error-text">{errors.email}</div>}
              </div>

              {/* Password Field */}
              <div className="cs-form-field">
                <label className="cs-field-label" htmlFor="password-input">Password</label>
                <div className={`cs-input-box ${errors.password ? 'error' : ''}`}>
                  <Lock className="cs-input-icon-left" />
                  <input
                    id="password-input"
                    type={showPassword ? 'text' : 'password'}
                    className="cs-input-control has-toggle"
                    placeholder="Enter your password"
                    value={form.password}
                    onChange={(e) => {
                      setForm({ ...form, password: e.target.value });
                      if (errors.password) setErrors({ ...errors, password: null });
                    }}
                  />
                  <button
                    type="button"
                    className="cs-toggle-eye-btn"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? "Hide password" : "Show password"}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {errors.password && <div className="cs-field-error-text">{errors.password}</div>}
              </div>

              {/* Forgot Password */}
              <div className="cs-forgot-row">
                <a href="#forgot" onClick={(e) => { e.preventDefault(); alert("Password reset link will be sent to your email."); }} className="cs-forgot-link">
                  Forgot password?
                </a>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                className="cs-btn-submit-primary"
                disabled={loading}
              >
                {loading ? <span className="cs-btn-spinner" /> : 'Sign In'}
              </button>
            </form>

            {/* Or Divider */}
            <div className="cs-auth-divider">
              <span className="cs-divider-line" />
              <span className="cs-divider-text">or continue with</span>
              <span className="cs-divider-line" />
            </div>

            {/* Social Login Buttons */}
            <div className="cs-social-buttons">
              <button
                type="button"
                className="cs-btn-social"
                onClick={() => handleSocialLogin('Google')}
              >
                <svg className="cs-social-icon" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" />
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" />
                </svg>
                Continue with Google
              </button>

              <button
                type="button"
                className="cs-btn-social"
                onClick={() => handleSocialLogin('GitHub')}
              >
                <svg className="cs-social-icon" viewBox="0 0 24 24" fill="#0f172a">
                  <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0024 12c0-6.63-5.37-12-12-12z" />
                </svg>
                Continue with GitHub
              </button>
            </div>

            {/* Footer Sign Up Link */}
            <p className="cs-auth-footer-text">
              Don't have an account? <Link to="/register">Sign up</Link>
            </p>

          </div>
        </div>

      </div>
    </div>
  );
}
