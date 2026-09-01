import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ||
  (import.meta.env.DEV ? 'http://localhost:8080' : 'https://codesense-ai-tuo7.onrender.com');

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Auto-logout on 401 / 403 — clears stale or invalid tokens
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      const token = localStorage.getItem('token');
      // Only auto-redirect if a token was present (authenticated session gone bad)
      if (token) {
        localStorage.removeItem('token');
        // Avoid redirect loop on the login page itself
        if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register')) {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authApi = {
  login: (data) => api.post('/api/auth/login', data, { timeout: 10000 }),
  register: (data) => api.post('/api/auth/signup', data, { timeout: 10000 }),
  socialLogin: (data) => api.post('/api/auth/social-login', data, { timeout: 10000 }),
  legacyLogin: (data) => api.post('/api/auth/legacy-login', data, { timeout: 10000 }),
  getMe: () => api.get('/api/auth/me', { timeout: 10000 })
};

// Project API
export const projectApi = {
  list: () => api.get('/api/projects'),
  getProjects: () => api.get('/api/projects'),
  get: (id) => api.get(`/api/projects/${id}`),
  getProject: (id) => api.get(`/api/projects/${id}`),
  create: (data) => api.post('/api/projects', data),
  createProject: (data) => api.post('/api/projects', data),
  delete: (id) => api.delete(`/api/projects/${id}`)
};

// Repository API
export const repoApi = {
  importGitHub: (projectId, data) => api.post(`/api/projects/${projectId}/repositories/github`, data),
  uploadZip: (projectId, formData) => api.post(`/api/projects/${projectId}/repositories/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),
  list: (projectId) => api.get(`/api/projects/${projectId}/repositories`),
  getRepositories: (projectId) => api.get(`/api/projects/${projectId}/repositories`),
  get: (repositoryId) => api.get(`/api/repositories/${repositoryId}`),
  getRepository: (repositoryId) => api.get(`/api/repositories/${repositoryId}`),
  getFiles: (repositoryId) => api.get(`/api/repositories/${repositoryId}/files`),
  files: (repositoryId) => api.get(`/api/repositories/${repositoryId}/files`),
  getFile: (repositoryId, fileId) => api.get(`/api/repositories/${repositoryId}/files/${fileId}`),
  file: (repositoryId, fileId) => api.get(`/api/repositories/${repositoryId}/files/${fileId}`),
  update: (repositoryId, data) => api.put(`/api/repositories/${repositoryId}`, data),
  refresh: (repositoryId) => api.post(`/api/repositories/${repositoryId}/refresh`),
  delete: (repositoryId) => api.delete(`/api/repositories/${repositoryId}`),
  deleteRepository: (repositoryId) => api.delete(`/api/repositories/${repositoryId}`)
};
export const repositoryApi = repoApi;

// AI API
export const aiApi = {
  health: () => api.get('/api/ai/health'),
  ingest: (data) => {
    const payload = typeof data === 'object' ? data : { repositoryId: data };
    return api.post('/api/ai/ingest', payload);
  },
  chat: (data) => api.post('/api/ai/chat', data, { timeout: 120000 }),
  search: (arg1, arg2, arg3, arg4) => {
    let payload;
    if (typeof arg1 === 'object' && arg1 !== null) {
      payload = {
        projectId: arg1.projectId,
        repositoryId: arg1.repositoryId,
        query: arg1.query,
        topK: arg1.topK || arg1.limit || 5
      };
    } else {
      payload = {
        projectId: arg1,
        repositoryId: arg2,
        query: arg3,
        topK: arg4 || 5
      };
    }
    return api.post('/api/ai/search', payload, { timeout: 60000 });
  },
  explainCode: (data) => api.post('/api/ai/explain-code', data, { timeout: 120000 }),
  generateReadme: (arg1, arg2) => {
    const payload = (typeof arg1 === 'object' && arg1 !== null)
      ? { projectId: arg1.projectId, repositoryId: arg1.repositoryId }
      : { projectId: arg1, repositoryId: arg2 };
    return api.post('/api/ai/generate-readme', payload, { timeout: 180000 });
  },
  generateApiDocs: (arg1, arg2) => {
    const payload = (typeof arg1 === 'object' && arg1 !== null)
      ? { projectId: arg1.projectId, repositoryId: arg1.repositoryId }
      : { projectId: arg1, repositoryId: arg2 };
    return api.post('/api/ai/generate-api-docs', payload, { timeout: 180000 });
  },
  getConversations: (projectId, repositoryId) => api.get(`/api/ai/conversations?projectId=${projectId}&repositoryId=${repositoryId}`),
  getMessages: (conversationId) => api.get(`/api/ai/conversations/${conversationId}/messages`),
  analyzeQuality: (projectId, repositoryId) => api.post('/api/ai/analyze-quality',
    { projectId, repositoryId }, { timeout: 180000 })
};

// Parser & Metrics API
export const parserApi = {
  parseRepository: (repositoryId) => api.post(`/api/parser/repositories/${repositoryId}/parse`, {}, { timeout: 180000 }),
  getMetrics: (repositoryId) => api.get(`/api/parser/repositories/${repositoryId}/metrics`, { timeout: 180000 }),
  // direction optional query param
  getDependencyGraph: (repositoryId, direction) => {
    const url = direction ? `/api/parser/repositories/${repositoryId}/dependency-graph?direction=${encodeURIComponent(direction)}` : `/api/parser/repositories/${repositoryId}/dependency-graph`;
    return api.post(url, {}, { timeout: 180000 });
  },
  startDependencyAnalysis: (repositoryId) => api.post(`/api/parser/repositories/${repositoryId}/dependency-analysis`, {}, { timeout: 180000 }),
  getDependencyAnalysis: (repositoryId, jobId) => api.get(`/api/parser/repositories/${repositoryId}/dependency-analysis/${jobId}`, { timeout: 60000 }),
  getUmlDiagrams: (repositoryId) => api.post(`/api/parser/repositories/${repositoryId}/uml`, {}, { timeout: 180000 }),
  getArchitectureDiagrams: (repositoryId) => api.post(`/api/parser/repositories/${repositoryId}/architecture`, {}, { timeout: 180000 }),
  startArchitectureAnalysis: (repositoryId) => api.post(`/api/parser/repositories/${repositoryId}/architecture-analysis`, {}, { timeout: 180000 }),
  getArchitectureAnalysis: (repositoryId, jobId) => api.get(`/api/parser/repositories/${repositoryId}/architecture-analysis/${jobId}`, { timeout: 60000 })
};
