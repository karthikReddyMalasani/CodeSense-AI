import React from 'react';

export default class AppErrorBoundary extends React.Component {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Unhandled application render error:', error, errorInfo);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <main className="app-error-fallback" role="alert">
        <div className="app-error-panel">
          <span className="eyebrow">CodeSense AI</span>
          <h1>This screen could not be loaded</h1>
          <p>The application hit an unexpected error. Reload to try again, or return to sign in if your session expired.</p>
          <div className="app-error-actions">
            <button className="btn btn-primary" type="button" onClick={this.handleReload}>Reload application</button>
            <a className="btn btn-secondary" href="/login">Go to sign in</a>
          </div>
          {import.meta.env.DEV && this.state.error?.message && (
            <details className="app-error-details">
              <summary>Technical details</summary>
              <pre>{this.state.error.message}</pre>
            </details>
          )}
        </div>
      </main>
    );
  }
}
