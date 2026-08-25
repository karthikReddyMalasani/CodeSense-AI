import React, { useState } from 'react';
import { useAI } from '../../context/AIContext';
import { useProject } from '../../context/ProjectContext';
import { useEditor } from '../../context/EditorContext';
import { Sparkles, Send, X, Code, Bug, Zap, TestTube, FileText, Check, Bot, User, ArrowRightLeft } from 'lucide-react';

const AIAssistant = () => {
  const { isAIPanelOpen, setIsAIPanelOpen, messages, sendMessage, isAILoading, executeQuickAction } = useAI();
  const { activeFile } = useProject();
  const { activeLanguage } = useEditor();
  const [inputText, setInputText] = useState('');
  const [targetLanguage, setTargetLanguage] = useState('python');

  if (!isAIPanelOpen) return null;

  const handleSend = (e) => {
    e.preventDefault();
    if (inputText.trim() && !isAILoading) {
      sendMessage(inputText.trim(), activeFile?.content || '', activeLanguage);
      setInputText('');
    }
  };

  return (
    <aside className="ai-assistant-drawer">
      <div className="ai-drawer-header">
        <div className="ai-header-title">
          <Sparkles className="ai-header-icon glow" />
          <span className="ai-title-text">CodeAssist AI</span>
        </div>
        <button className="icon-close-btn" onClick={() => setIsAIPanelOpen(false)}>
          <X className="close-icon" />
        </button>
      </div>

      <div className="ai-quick-actions-bar">
        <span className="quick-title">QUICK ACTIONS</span>
        <div className="quick-buttons-grid">
          <button
            className="quick-btn"
            onClick={() => executeQuickAction('explain', activeFile?.content || '', activeLanguage)}
          >
            <Code className="q-icon" /> Explain Code
          </button>

          <button
            className="quick-btn"
            onClick={() => executeQuickAction('debug', activeFile?.content || '', activeLanguage)}
          >
            <Bug className="q-icon" /> Find Bug
          </button>

          <button
            className="quick-btn"
            onClick={() => executeQuickAction('optimize', activeFile?.content || '', activeLanguage)}
          >
            <Zap className="q-icon" /> Optimize
          </button>

          <button
            className="quick-btn"
            onClick={() => executeQuickAction('tests', activeFile?.content || '', activeLanguage)}
          >
            <TestTube className="q-icon" /> Generate Tests
          </button>

          <button
            className="quick-btn"
            onClick={() => executeQuickAction('document', activeFile?.content || '', activeLanguage)}
          >
            <FileText className="q-icon" /> Document
          </button>

          <div className="convert-action-group">
            <button
              className="quick-btn"
              onClick={() => executeQuickAction('convert', `Convert to ${targetLanguage}:\n\n${activeFile?.content || ''}`, activeLanguage)}
            >
              <ArrowRightLeft className="q-icon" /> Convert
            </button>
            <select
              className="target-lang-select"
              value={targetLanguage}
              onChange={(e) => setTargetLanguage(e.target.value)}
            >
              <option value="python">Python</option>
              <option value="java">Java</option>
              <option value="javascript">JavaScript</option>
              <option value="cpp">C++</option>
              <option value="go">Go</option>
            </select>
          </div>
        </div>
      </div>

      {/* Selected Code Context Bar */}
      {activeFile && (
        <div className="selected-code-context">
          <span className="context-label">Active File Context:</span>
          <span className="context-file">{activeFile.name}</span>
          <div className="context-actions">
            <button
              className="context-btn"
              onClick={() => executeQuickAction('explain', activeFile.content, activeLanguage)}
            >
              Explain Selected Code
            </button>
            <button
              className="context-btn"
              onClick={() => executeQuickAction('debug', activeFile.content, activeLanguage)}
            >
              Fix Selected Code
            </button>
            <button
              className="context-btn"
              onClick={() => executeQuickAction('optimize', activeFile.content, activeLanguage)}
            >
              Optimize Selected Code
            </button>
          </div>
        </div>
      )}

      {/* Messages Feed */}
      <div className="ai-chat-messages">
        {messages.map((msg) => (
          <div key={msg.id} className={`chat-bubble-wrapper ${msg.sender}`}>
            <div className="avatar">
              {msg.sender === 'ai' ? <Bot className="bot-icon glow" /> : <User className="user-icon" />}
            </div>
            <div className="bubble-content">
              <div className="bubble-header">
                <span className="sender-name">{msg.sender === 'ai' ? 'CodeAssist AI' : 'You'}</span>
                <span className="msg-time">{msg.timestamp}</span>
              </div>
              <pre className="msg-text">{msg.text}</pre>
            </div>
          </div>
        ))}

        {isAILoading && (
          <div className="chat-bubble-wrapper ai loading">
            <div className="avatar">
              <Bot className="bot-icon spin" />
            </div>
            <div className="bubble-content">
              <span className="typing-dots">CodeAssist AI is thinking...</span>
            </div>
          </div>
        )}
      </div>

      {/* Input Area */}
      <form onSubmit={handleSend} className="ai-input-form">
        <input
          type="text"
          className="ai-input"
          placeholder="Ask CodeAssist AI..."
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
        />
        <button type="submit" className="btn-send-ai" disabled={isAILoading || !inputText.trim()}>
          <Send className="send-icon" />
        </button>
      </form>
    </aside>
  );
};

export default AIAssistant;
