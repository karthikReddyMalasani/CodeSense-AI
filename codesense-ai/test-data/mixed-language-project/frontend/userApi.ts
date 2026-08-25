import UserService from './userService';
import AuthService from './auth';

interface ApiClient {
    baseUrl: string;
    getHeaders(): Record<string, string>;
}

class RestApiClient implements ApiClient {
    baseUrl: string;
    private token: string | null = null;

    constructor(baseUrl: string) {
        this.baseUrl = baseUrl;
    }

    getHeaders(): Record<string, string> {
        const headers: Record<string, string> = {
            'Content-Type': 'application/json'
        };
        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }
        return headers;
    }

    async login(email: string, password: string): Promise<void> {
        const res = await fetch(`${this.baseUrl}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        this.token = data.token;
    }
}

export { ApiClient, RestApiClient };
