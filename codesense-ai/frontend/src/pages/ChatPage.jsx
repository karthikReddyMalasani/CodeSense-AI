import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import SimpleMarkdown from '../components/common/SimpleMarkdown';
import { repositoryApi, aiApi } from '../services/api';
import ProjectSubNav from '../components/common/ProjectSubNav';

export default function ChatPage() {
  const { id: projectId } = useParams();
  const [repositories, setRepositories] = useState([]);
  const [selectedRepo, setSelectedRepo] = useState(null);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [ingesting, setIngesting] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [conversationId, setConversationId] = useState(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    repositoryApi.list(projectId).then(res => {
      const repos = (res.data.data || []).filter(r => r.status === 'READY');
      setRepositories(repos);
      if (repos.length > 0) setSelectedRepo(repos[0]);
    }).catch(() => { });
  }, [projectId]);

  useEffect(() => {
    if (!selectedRepo || selectedRepo.ingestionStatus === 'COMPLETED') return undefined;
    const interval = setInterval(() => {
      repositoryApi.list(projectId).then(res => {
        const repos = (res.data.data || []).filter(r => r.status === 'READY');
        setRepositories(repos);
        setSelectedRepo(current => repos.find(repo => repo.id === current?.id) || current);
      }).catch(() => { });
    }, 4000);
    return () => clearInterval(interval);
  }, [projectId, selectedRepo]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || !selectedRepo || loading) return;

    if (selectedRepo.status !== 'READY' || selectedRepo.ingestionStatus !== 'COMPLETED') {
      const blockingMessage = 'This repository is not ready for AI chat yet. Please wait for ingestion to complete or trigger AI ingestion.';
      setStatusMessage(blockingMessage);
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: blockingMessage,
        error: true
      }]);
      return;
    }

    const question = input.trim();
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: question }]);
    setLoading(true);
    setStatusMessage('');

    try {
      const res = await aiApi.chat({
        projectId, repositoryId: selectedRepo.id, conversationId, question
      });
      const data = res.data.data || res.data || {};
      setConversationId(data.conversationId || conversationId);
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: data.answer || 'I could not generate a response for this question.',
        sources: data.sources || []
      }]);
    } catch (err) {
      const message = err.response?.data?.message || 'Sorry, I encountered an error processing your question. Please try again.';
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: message,
        error: true
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
  };

  const newConversation = () => {
    setMessages([]);
    setConversationId(null);
  };

  const startIngestion = async () => {
    if (!selectedRepo || ingesting) return;
    setIngesting(true);
    setStatusMessage('Repository indexing started. This page will be ready for questions when ingestion completes.');
    try {
      await aiApi.ingest({ projectId, repositoryId: selectedRepo.id });
    } catch (err) {
      setStatusMessage(err.response?.data?.message || 'Could not start repository indexing. Please try again.');
    } finally {
      setIngesting(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <div>
          <div className="page-title">💬 Repository Chat</div>
          <div className="page-subtitle">Ask questions about your codebase with AI RAG</div>
        </div>
        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          {repositories.length > 0 && (
            <select className="input" style={{ width: 'auto' }}
              value={selectedRepo?.id || ''} onChange={e => {
                const r = repositories.find(r => r.id === e.target.value);
                setSelectedRepo(r);
                newConversation();
              }}>
              {repositories.map(r => <option key={r.id} value={r.id}>{r.name}</option>)}
            </select>
          )}
          <button className="btn btn-secondary btn-sm" onClick={newConversation}>New Chat</button>
        </div>
      </div>

      <ProjectSubNav activeTab="chat" />

      {selectedRepo && selectedRepo.ingestionStatus !== 'COMPLETED' && (
        <div className="alert alert-info" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <span>
            {statusMessage || `AI chat is waiting for repository indexing. Current status: ${selectedRepo.ingestionStatus || 'PENDING'}.`}
          </span>
          <button className="btn btn-secondary btn-sm" onClick={startIngestion} disabled={ingesting}>
            {ingesting ? 'Indexing...' : '🔄 Ingest AI'}
          </button>
        </div>
      )}

      {
        repositories.length === 0 ? (
          <div className="empty-state">
            <h3>No ready repositories</h3>
            <p>Upload a repository and trigger AI ingestion before chatting.</p>
          </div>
        ) : (
          <div className="card" style={{ padding: 0, display: 'flex', flexDirection: 'column', height: 'calc(100vh - 240px)' }}>
            {/* Messages */}
            <div style={{ flex: 1, overflowY: 'auto', padding: '20px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {messages.length === 0 && (
                <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '60px' }}>
                  <div style={{ fontSize: '32px', marginBottom: '12px' }}>💬</div>
                  <div style={{ fontWeight: '600', color: 'var(--text)' }}>Start a conversation</div>
                  <div style={{ fontSize: '13px', marginTop: '8px' }}>
                    Ask about classes, methods, authentication, architecture, or anything in your repository.
                  </div>
                  <div style={{ marginTop: '16px', display: 'flex', gap: '8px', justifyContent: 'center', flexWrap: 'wrap' }}>
                    {['How does authentication work?', 'Explain the main classes', 'What APIs are available?'].map(q => (
                      <button key={q} className="btn btn-secondary btn-sm"
                        onClick={() => { setInput(q); }}>
                        {q}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {messages.map((msg, i) => (
                <div key={i} className={`chat-message ${msg.role}`}>
                  <div className="message-avatar">
                    {msg.role === 'user' ? '👤' : '🤖'}
                  </div>
                  <div>
                    <div className="message-content">
                      {msg.role === 'assistant' ? (
                        <div className="markdown">
                          <SimpleMarkdown>{msg.content}</SimpleMarkdown>
                        </div>
                      ) : msg.content}
                    </div>
                    {msg.sources && msg.sources.length > 0 && (
                      <div className="message-sources">
                        <div className="message-sources-title">📎 Sources:</div>
                        {msg.sources.map((s, j) => (
                          <div key={j} className="source-item">
                            {s.filePath}{s.startLine ? ` (lines ${s.startLine}–${s.endLine})` : ''}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              ))}

              {loading && (
                <div className="chat-message assistant">
                  <div className="message-avatar">🤖</div>
                  <div className="message-content">
                    <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }}>
                      <div className="spinner" style={{ width: '14px', height: '14px' }} />
                      <span style={{ color: 'var(--text-muted)', fontSize: '12px' }}>Thinking...</span>
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="chat-input-area">
              <div className="chat-input-row">
                <textarea className="chat-textarea" rows={1} placeholder="Ask a question about your repository..."
                  value={input} onChange={e => setInput(e.target.value)} onKeyDown={handleKeyDown} />
                <button className="btn btn-primary" onClick={sendMessage} disabled={loading || !input.trim()}>
                  Send
                </button>
              </div>
              <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginTop: '6px' }}>
                Press Enter to send · Shift+Enter for new line
              </div>
            </div>
          </div>
        )
      }
    </div >
  );
}
