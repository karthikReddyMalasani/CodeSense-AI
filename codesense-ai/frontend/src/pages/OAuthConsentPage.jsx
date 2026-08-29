import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import '../styles/OAuthConsentPage.css';

export default function OAuthConsentPage() {
  const [searchParams] = useSearchParams();
  const { user, token } = useAuth();
  const clientName = searchParams.get('client_name') || searchParams.get('client_id') || 'An OAuth application';
  const scope = searchParams.get('scope')?.split(' ').filter(Boolean) || ['Account access'];
  const redirectUri = searchParams.get('redirect_uri');

  const finish = (approved) => {
    if (!redirectUri) return;
    const url = new URL(redirectUri);
    url.searchParams.set('consent', approved ? 'approved' : 'denied');
    const state = searchParams.get('state');
    if (state) url.searchParams.set('state', state);
    window.location.assign(url.toString());
  };

  return (
    <main className="oauth-consent-page">
      <section className="oauth-consent-card" aria-labelledby="consent-title">
        <div className="oauth-consent-mark"><ShieldCheck size={28} /></div>
        <p className="oauth-consent-eyebrow">CodeSense AI authorization</p>
        <h1 id="consent-title">Allow {clientName} to connect?</h1>
        <p className="oauth-consent-copy">
          Review the requested access before continuing. You can revoke access later from your account settings.
        </p>

        <div className="oauth-consent-account">
          <span>Signed in as</span>
          <strong>{user?.email || 'No CodeSense account selected'}</strong>
        </div>

        <ul className="oauth-consent-scopes">
          {scope.map((item) => <li key={item}>{item.replace(/_/g, ' ')}</li>)}
        </ul>

        <div className="oauth-consent-actions">
          <button type="button" className="oauth-consent-deny" onClick={() => finish(false)}>Deny</button>
          <button type="button" className="oauth-consent-allow" onClick={() => finish(true)} disabled={!token || !redirectUri}>
            Allow access
          </button>
        </div>
        {!token && <p className="oauth-consent-note">Sign in to CodeSense AI before approving this request.</p>}
        {!redirectUri && <p className="oauth-consent-note">This authorization request is missing a redirect URL.</p>}
      </section>
    </main>
  );
}
