/**
 * Supabase Auth URL Error Handling Utilities
 */

/**
 * Parses authentication error parameters from window.location search query and hash parameters.
 * Returns a user-friendly error message string if errors are found, or null if no errors.
 */
export function parseAuthUrlErrors(searchStr, hashStr) {
    if (typeof window === 'undefined') return null;

    const search = searchStr !== undefined ? searchStr : window.location.search;
    const hash = hashStr !== undefined ? hashStr : window.location.hash;

    const urlParams = new URLSearchParams(search);
    const hashString = hash.startsWith('#') ? hash.slice(1) : hash;
    const hashParams = new URLSearchParams(hashString);

    const error = urlParams.get('error') || hashParams.get('error');
    const errorCode = urlParams.get('error_code') || hashParams.get('error_code');
    const rawDescription = urlParams.get('error_description') || hashParams.get('error_description');

    if (!error && !errorCode && !rawDescription) {
        return null;
    }

    let description = rawDescription || '';
    try {
        description = decodeURIComponent(description).replace(/\+/g, ' ');
    } catch {
        description = description.replace(/\+/g, ' ');
    }

    // Handle specific known Supabase error messages and map to clean user-facing advice
    if (
        description.includes('Multiple accounts with the same email address') ||
        description.includes('linking domain') ||
        errorCode === 'unexpected_failure' ||
        (error === 'server_error' && rawDescription?.includes('Multiple accounts'))
    ) {
        return 'An account with this email address already exists using a different sign-in method. Please sign in using your original email & password or social login provider.';
    }

    if (error === 'access_denied' || errorCode === 'access_denied') {
        return 'Authentication request was cancelled or access was denied.';
    }

    if (description.includes('expired') || description.includes('invalid')) {
        return 'Authentication or email link has expired. Please request a new verification link.';
    }

    return description || error || 'Authentication failed. Please try logging in again.';
}

/**
 * Strips authentication error parameters from the browser location bar without causing a page reload.
 */
export function cleanAuthUrlParams() {
    if (typeof window === 'undefined') return;

    const url = new URL(window.location.href);
    const searchParams = url.searchParams;

    // Remove common error query params
    searchParams.delete('error');
    searchParams.delete('error_code');
    searchParams.delete('error_description');
    searchParams.delete('sb');

    // Clear error parameters from hash if hash exists
    if (url.hash.includes('error=')) {
        url.hash = '';
    }

    const newUrl = url.pathname + (searchParams.toString() ? `?${searchParams.toString()}` : '') + url.hash;
    window.history.replaceState({}, document.title, newUrl);
}
