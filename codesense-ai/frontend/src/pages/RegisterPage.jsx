import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { User, Mail, Lock, Eye, EyeOff } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import '../styles/LoginPage.css';

export default function RegisterPage() {
  const { signUpWithVerification, sendMagicLink, socialLogin } = useAuth();
  const navigate = useNavigate();

  // step: 'form' | 'verify-email' | 'done'
  const [step, setStep] = useState('form');
  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [errors, setErrors] = useState({});
  const [globalError, setGlobalError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSocialLogin = async (provider) => {
    setGlobalError('');
    setLoading(true);
    try {
      const demoEmail = form.email.trim() || `${provider.toLowerCase()}_developer@codesense.ai`;
      const demoName = form.name.trim() || `${provider} Developer`;
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

  const validateForm = () => {
    const newErrors = {};
    if (!form.name.trim()) {
      newErrors.name = 'Full name is required';
    } else if (form.name.trim().length < 2) {
      newErrors.name = 'Name must be at least 2 characters';
    }
    if (!form.email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/\S+@\S+\.\S+/.test(form.email)) {
      newErrors.email = 'Please enter a valid email address';
    }
    if (!form.password) {
      newErrors.password = 'Password is required';
    } else if (form.password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }
    if (!form.confirm) {
      newErrors.confirm = 'Please confirm your password';
    } else if (form.password !== form.confirm) {
      newErrors.confirm = 'Passwords do not match';
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
      // Always send verification email first — backend account created after email link click
      await signUpWithVerification(form.name, form.email, form.password);
      setStep('verify-email');
    } catch (err) {
      const errMsg = err.message || '';
      if (errMsg.toLowerCase().includes('already registered') || errMsg.toLowerCase().includes('already exist')) {
        setGlobalError('An account with this email already exists. Please sign in instead.');
      } else if (errMsg.includes('rate limit') || errMsg.includes('429')) {
        setGlobalError('Email rate limit reached. Please wait a few minutes and try again.');
      } else {
        setGlobalError(errMsg || 'Registration failed. Please check your details and try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResendLink = async () => {
    setGlobalError('');
    setLoading(true);
    try {
      await signUpWithVerification(form.name, form.email, form.password);
    } catch (err) {
      setGlobalError('Could not resend email. Please wait a moment and try again.');
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
              Start Building.<br />
              Smarter <span className="highlight-blue">Together.</span>
            </h1>
            <p className="cs-branding-subtext">
              Join thousands of developers using CodeSense AI to understand complex codebases, automate documentation, and ship faster.
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

        {/* RIGHT COLUMN — REGISTER CARD */}
        <div className="cs-login-form-panel">
          <div className="cs-login-form-card" style={{ padding: '36px 32px' }}>

            {/* Header */}
            <div className="cs-form-header" style={{ marginBottom: '20px' }}>
              <h2 className="cs-form-title" style={{ fontSize: '26px' }}>Create Account</h2>
              <p className="cs-form-subtitle">Get started with CodeSense AI in seconds</p>
            </div>

            {/* Global Error Banner */}
            {globalError && (
              <div className="cs-global-alert" style={{ marginBottom: '16px' }}>
                {globalError}
              </div>
            )}

            {/* Form / Verify Email */}
            {step === 'form' ? (
              <form onSubmit={handleSubmit} className="cs-auth-form" noValidate style={{ gap: '14px' }}>
                {/* Full Name Field */}
                <div className="cs-form-field">
                  <label className="cs-field-label" htmlFor="reg-name">Full Name</label>
                  <div className={`cs-input-box ${errors.name ? 'error' : ''}`}>
                    <User className="cs-input-icon-left" />
                    <input
                      id="reg-name"
                      type="text"
                      className="cs-input-control"
                      placeholder="John Doe"
                      value={form.name}
                      onChange={(e) => {
                        setForm({ ...form, name: e.target.value });
                        if (errors.name) setErrors({ ...errors, name: null });
                      }}
                    />
                  </div>
                  {errors.name && <div className="cs-field-error-text">{errors.name}</div>}
                </div>

                {/* Email Field */}
                <div className="cs-form-field">
                  <label className="cs-field-label" htmlFor="reg-email">Email Address</label>
                  <div className={`cs-input-box ${errors.email ? 'error' : ''}`}>
                    <Mail className="cs-input-icon-left" />
                    <input
                      id="reg-email"
                      type="email"
                      className="cs-input-control"
                      placeholder="you@example.com"
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
                  <label className="cs-field-label" htmlFor="reg-password">Password</label>
                  <div className={`cs-input-box ${errors.password ? 'error' : ''}`}>
                    <Lock className="cs-input-icon-left" />
                    <input
                      id="reg-password"
                      type={showPassword ? 'text' : 'password'}
                      className="cs-input-control has-toggle"
                      placeholder="Min. 8 characters"
                      value={form.password}
                      onChange={(e) => {
                        setForm({ ...form, password: e.target.value });
                        if (errors.password) setErrors({ ...errors, password: null });
                      }}
                    />
                    <button type="button" className="cs-toggle-eye-btn" onClick={() => setShowPassword(!showPassword)}>
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                  {errors.password && <div className="cs-field-error-text">{errors.password}</div>}
                </div>

                {/* Confirm Password Field */}
                <div className="cs-form-field">
                  <label className="cs-field-label" htmlFor="reg-confirm">Confirm Password</label>
                  <div className={`cs-input-box ${errors.confirm ? 'error' : ''}`}>
                    <Lock className="cs-input-icon-left" />
                    <input
                      id="reg-confirm"
                      type={showConfirm ? 'text' : 'password'}
                      className="cs-input-control has-toggle"
                      placeholder="Repeat password"
                      value={form.confirm}
                      onChange={(e) => {
                        setForm({ ...form, confirm: e.target.value });
                        if (errors.confirm) setErrors({ ...errors, confirm: null });
                      }}
                    />
                    <button type="button" className="cs-toggle-eye-btn" onClick={() => setShowConfirm(!showConfirm)}>
                      {showConfirm ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>
                  {errors.confirm && <div className="cs-field-error-text">{errors.confirm}</div>}
                </div>

                <button type="submit" className="cs-btn-submit-primary" disabled={loading} style={{ marginTop: '8px' }}>
                  {loading ? <span className="cs-btn-spinner" /> : 'Create Account'}
                </button>
              </form>
            ) : (
              /* Verify Email Step */
              <div className="cs-auth-form" style={{ gap: '20px', display: 'flex', flexDirection: 'column' }}>
                <div style={{
                  background: 'rgba(16, 185, 129, 0.08)',
                  border: '1px solid rgba(16, 185, 129, 0.25)',
                  padding: '16px',
                  borderRadius: '10px',
                  display: 'flex',
                  gap: '12px',
                  alignItems: 'flex-start'
                }}>
                  <span style={{ fontSize: '22px' }}>📩</span>
                  <div>
                    <div style={{ fontWeight: '600', marginBottom: '4px', color: 'var(--cs-text-main)' }}>Check your email</div>
                    <div style={{ fontSize: '13px', color: 'var(--cs-text-muted)', lineHeight: '1.5' }}>
                      We sent a secure sign-in link to <strong>{form.email}</strong>. Click the link in the email to verify your account and log in.
                    </div>
                  </div>
                </div>

                <div style={{ fontSize: '13px', color: 'var(--cs-text-muted)', textAlign: 'center' }}>
                  Didn't receive an email?{' '}
                  <button
                    type="button"
                    className="cs-btn-link"
                    style={{ fontWeight: '600', textDecoration: 'underline' }}
                    onClick={handleResendLink}
                    disabled={loading}
                  >
                    {loading ? 'Sending...' : 'Resend link'}
                  </button>
                </div>

                <button
                  type="button"
                  className="cs-btn-social"
                  onClick={() => setStep('form')}
                  style={{ width: '100%', justifyContent: 'center' }}
                >
                  ← Back to registration
                </button>
              </div>
            )}

            {/* Or Divider */}
            <div className="cs-auth-divider" style={{ marginTop: '16px' }}>
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

            {/* Footer Sign In Link */}
            <p className="cs-auth-footer-text" style={{ marginTop: '16px' }}>
              Already have an account? <Link to="/login">Sign in</Link>
            </p>

          </div>
        </div>

      </div>
    </div>
  );
}
