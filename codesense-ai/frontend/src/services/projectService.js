import { api } from './api';
import { DEFAULT_PROJECT, INITIAL_RECENT_PROJECTS } from '../utils/constants';

export const projectService = {
  async getProjects() {
    try {
      const response = await api.get('/api/projects');
      return { success: true, isDemo: false, projects: response.data };
    } catch (err) {
      return { success: true, isDemo: true, projects: INITIAL_RECENT_PROJECTS };
    }
  },

  async getProjectById(projectId) {
    try {
      const response = await api.get(`/api/projects/${projectId}`);
      return { success: true, isDemo: false, project: response.data };
    } catch (err) {
      return { success: true, isDemo: true, project: DEFAULT_PROJECT };
    }
  },

  async createProject(projectData) {
    try {
      const response = await api.post('/api/projects', projectData);
      return { success: true, isDemo: false, project: response.data };
    } catch (err) {
      const newProj = {
        id: 'proj-' + Date.now(),
        name: projectData.name || 'New Project',
        language: projectData.language || 'java',
        fileCount: 1,
        lastModified: 'Just now',
        description: projectData.description || 'Created in CodeAssist AI workspace'
      };
      return { success: true, isDemo: true, project: newProj };
    }
  }
};
