import React, { createContext, useContext, useState } from 'react';
import { aiService } from '../services/aiService';

const AIContext = createContext();

export const AIProvider = ({ children }) => {
  const [isAIPanelOpen, setIsAIPanelOpen] = useState(false);
  const [isAILoading, setIsAILoading] = useState(false);
  const [messages, setMessages] = useState([
    {
      id: 'm1',
      sender: 'ai',
      text: 'Hello! I am **CodeAssist AI**. How can I help you analyze, debug, or optimize your code today?',
      timestamp: '12:00 PM'
    }
  ]);

  const toggleAIPanel = () => setIsAIPanelOpen((prev) => !prev);

  const sendMessage = async (text, codeContext = '', language = 'java') => {
    if (!text.trim()) return;

    const userMsg = {
      id: 'm-' + Date.now(),
      sender: 'user',
      text,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, userMsg]);
    setIsAILoading(true);

    const response = await aiService.askAI(text, codeContext, language);

    const aiMsg = {
      id: 'm-ai-' + Date.now(),
      sender: 'ai',
      text: response.reply,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, aiMsg]);
    setIsAILoading(false);
  };

  const executeQuickAction = async (actionType, codeContext, language) => {
    setIsAIPanelOpen(true);
    let prompt = '';

    if (actionType === 'explain') prompt = 'Explain this code in detail with step-by-step logic.';
    else if (actionType === 'debug') prompt = 'Find bugs or potential runtime exceptions in this code.';
    else if (actionType === 'optimize') prompt = 'Suggest performance and memory optimizations for this code.';
    else if (actionType === 'tests') prompt = 'Generate comprehensive unit test cases for this code.';
    else if (actionType === 'docs') prompt = 'Generate documentation and function specifications for this code.';

    await sendMessage(prompt, codeContext, language);
  };

  return (
    <AIContext.Provider
      value={{
        isAIPanelOpen,
        setIsAIPanelOpen,
        toggleAIPanel,
        isAILoading,
        messages,
        sendMessage,
        executeQuickAction
      }}
    >
      {children}
    </AIContext.Provider>
  );
};

export const useAI = () => useContext(AIContext);
