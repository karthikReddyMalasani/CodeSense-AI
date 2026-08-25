# CodeSense AI Frontend

## React 18

### Quick Start

```bash
npm install
npm start
```

**URL**: `http://localhost:3000`  
**Backend**: `http://localhost:8080` (configured via proxy in package.json)

### Pages

| Path | Page |
|------|------|
| `/login` | Login |
| `/register` | Registration |
| `/dashboard` | Overview dashboard |
| `/projects` | Project list |
| `/projects/:id` | Project detail + repository management |
| `/projects/:id/repository` | File browser |
| `/projects/:id/chat` | AI chatbot (RAG) |
| `/projects/:id/search` | Semantic search |
| `/projects/:id/code-explanation` | Code explanation |
| `/projects/:id/readme` | README generator |
| `/projects/:id/api-docs` | API docs generator |
| `/projects/:id/architecture` | Architecture view (Team Member 4 placeholder) |

### Environment

```
REACT_APP_API_BASE_URL=http://localhost:8080
```
