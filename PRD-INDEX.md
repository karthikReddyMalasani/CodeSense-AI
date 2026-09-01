# Product Requirements Documents (PRDs) — Index

## Overview
This directory contains comprehensive Product Requirements Documents for CodeSense AI covering both Frontend UI and Backend API specifications.

---

## 📄 Available PRDs

### 1. **Frontend UI PRD** 
📎 [PRD-FRONTEND.md](PRD-FRONTEND.md)

**Scope**: React-based web application  
**Audience**: Frontend developers, UI/UX designers, product managers

**Key Sections**:
- Authentication & Authorization (Login, Registration, Profiles)
- Dashboard & Navigation
- Repository Management & File Browser
- AI Features (Chat, Search, Code Explanation)
- Analysis & Documentation (Metrics, README Generator, API Docs)
- Architecture Visualization (UML, Dependencies)
- Technical Requirements (Framework, Performance, Accessibility)
- Non-Functional Requirements (Security, Reliability, Scalability)
- Success Metrics & Roadmap

**Quick Stats**:
- 14 main pages/features
- Performance targets: Page load < 2s, Search < 3s
- 50+ supported languages for syntax highlighting
- WCAG 2.1 Level AA accessibility compliance

---

### 2. **Backend API PRD**
📎 [PRD-BACKEND-API.md](PRD-BACKEND-API.md)

**Scope**: Spring Boot REST API microservices  
**Audience**: Backend developers, DevOps, API architects

**Key Sections**:
- Authentication APIs (Register, Login, Social Auth)
- Project Management APIs (CRUD operations)
- Repository Management APIs (Upload, GitHub import, File browsing)
- Parser & Code Analysis APIs (Metrics, Dependency graphs, UML, Architecture)
- AI Engine APIs (Chat, Search, Code Explanation, Documentation generation)
- Conversation Management APIs
- Non-Functional Requirements (Performance, Scalability, Security)
- Technology Stack (Spring Boot 3.x, PostgreSQL, PGVector)
- Deployment & Operations

**Quick Stats**:
- 50+ API endpoints (fully documented)
- Support for 13+ programming languages
- Multi-LLM provider support (IBM, Groq, Gemini, Ollama)
- PGVector-based semantic search
- Rate limiting, audit logging, access control

---

## 🎯 Key Features Documented

### Frontend Features
✅ User Authentication (Email/Password, Social Login)  
✅ Project & Repository Management  
✅ Interactive File Browser with Syntax Highlighting  
✅ AI-Powered Chat & Semantic Search  
✅ Code Explanation & Documentation Generation  
✅ Code Metrics Dashboard  
✅ Dependency Graph & Architecture Visualization  
✅ Quality Analysis & Recommendations  

### Backend Features
✅ JWT Authentication with Role-Based Access Control  
✅ Multi-Language Code Parsing (Java, Python, JS, etc.)  
✅ Semantic Search via PGVector Embeddings  
✅ RAG-Powered Chatbot with Citation Sources  
✅ Async Repository Ingestion Pipeline  
✅ Code Quality Metrics Calculation  
✅ Dependency Graph Analysis  
✅ Multiple LLM Provider Support  

---

## 📊 Document Structure

### Frontend PRD Contents
1. Executive Summary
2. Product Overview & Vision
3. Authentication & Authorization
4. Dashboard & Navigation
5. Repository Management
6. AI Features (Chat, Search, Explanation)
7. Analysis & Documentation
8. Architecture & Visualization
9. Quality Analysis
10. Settings & User Preferences
11. Technical Requirements
12. Non-Functional Requirements
13. Constraints & Assumptions
14. Success Metrics
15. Roadmap (Future Phases)

### Backend API PRD Contents
1. Executive Summary
2. Product Overview
3. Detailed API Specifications (with request/response examples)
   - Authentication (4 endpoints)
   - Project Management (4 endpoints)
   - Repository Management (7 endpoints)
   - Parser & Analysis (6 endpoints)
   - AI Engine (6 endpoints)
   - Conversation Management (2 endpoints)
4. Non-Functional Requirements
5. Technology Stack
6. Deployment & Operations
7. Acceptance Criteria Checklist
8. Roadmap (Future Phases)

---

## 🚀 Usage Guide

### For Frontend Developers
1. Read [PRD-FRONTEND.md](PRD-FRONTEND.md) sections 1-3 for overall context
2. Focus on sections 4-9 for feature specifications
3. Reference section 11 for technical implementation details
4. Use Appendix for color scheme, keyboard shortcuts

### For Backend Developers
1. Read [PRD-BACKEND-API.md](PRD-BACKEND-API.md) sections 1-2 for overall context
2. Focus on section 3 for complete API specifications
3. Use request/response examples as integration test fixtures
4. Reference sections 4-5 for non-functional requirements

### For Product Managers
1. Read both PRD Executive Summaries
2. Review Feature List and Success Metrics
3. Refer to Roadmap sections for planning
4. Use Constraints & Assumptions for scope management

### For QA/Testing Teams
1. Extract Acceptance Criteria from each feature section
2. Use API examples as test cases
3. Reference Non-Functional Requirements for performance testing
4. Use Error Response examples for negative test cases

---

## 📝 API Documentation Quick Reference

### Authentication Endpoints
- `POST /api/auth/register` — Register new user
- `POST /api/auth/login` — Login with email/password
- `POST /api/auth/social-login` — OAuth social login
- `GET /api/auth/me` — Get current user profile

### Project Endpoints
- `POST /api/projects` — Create project
- `GET /api/projects` — List user's projects
- `GET /api/projects/{projectId}` — Get project details
- `DELETE /api/projects/{projectId}` — Delete project

### Repository Endpoints
- `POST /api/projects/{projectId}/repositories/upload` — Upload ZIP
- `POST /api/projects/{projectId}/repositories/github` — Import from GitHub
- `GET /api/projects/{projectId}/repositories` — List repositories
- `GET /api/repositories/{repositoryId}` — Get repository details
- `GET /api/repositories/{repositoryId}/files` — List files
- `GET /api/repositories/{repositoryId}/files/{fileId}` — Get file content
- `DELETE /api/repositories/{repositoryId}` — Delete repository

### AI Endpoints
- `POST /api/ai/ingest` — Start repository ingestion
- `POST /api/ai/chat` — Chat with AI about repository
- `POST /api/ai/search` — Semantic code search
- `POST /api/ai/explain-code` — AI code explanation
- `POST /api/ai/generate-readme` — Auto-generate README
- `POST /api/ai/generate-api-docs` — Auto-generate API docs

### Analysis Endpoints
- `GET /api/parser/repositories/{repositoryId}/metrics` — Code metrics
- `POST /api/parser/repositories/{repositoryId}/dependency-graph` — Dependency graph
- `POST /api/parser/repositories/{repositoryId}/uml` — UML diagram
- `POST /api/parser/repositories/{repositoryId}/architecture` — Architecture diagram

---

## 🔄 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-09-01 | Initial PRD release with full feature specifications |

---

## 📞 Contact & Support

For questions about these PRDs:
- **Frontend**: Contact Team Member 1 (Frontend Lead)
- **Backend**: Contact Team Member 3 (Backend Lead)
- **Product**: Contact Product Manager
- **All**: Use project wiki or GitHub issues

---

## ✅ Checklist for Implementation

- [ ] PRD reviewed by development team
- [ ] Acceptance criteria extracted to issue tracking
- [ ] API endpoints tested with provided examples
- [ ] Frontend pages validated against specification
- [ ] Performance targets established as CI/CD gates
- [ ] Security requirements integrated into test suite
- [ ] Error messages match PRD examples
- [ ] Rate limiting configured per specification
- [ ] API documentation auto-generated from Swagger
- [ ] Monitoring alerts configured per metrics

---

*Last Updated: September 1, 2026*  
*For the latest version, refer to `/codesense-ai/docs/`*
