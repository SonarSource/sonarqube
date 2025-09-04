import React, { useEffect, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown'; // Import react-markdown
import { postRequest } from '../../api/codescan';
import './ChatWidget.css';
import '../styles/ChatWidget.css';

type Role = 'user' | 'assistant';
type Message = { role: Role; content: string };

type ChatbotAPIResponse = {
  answer?: string;
  content?: string;
  response?: string;
};

// Change this to your real endpoint or env var
const API_URL = '/_codescan/chatbot/query';

const ChatWidget: React.FC = () => {
  const [isOpen, setIsOpen] = useState(true);
  const [isMaximized, setIsMaximized] = useState(false); // Tracks if the widget is maximized
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const endRef = useRef<HTMLDivElement | null>(null);

  const toggleChat = () => {
    if (isOpen) {
      setIsMaximized(false); // Reset maximized state when closing the chat
    }
    setIsOpen((v) => !v);
  };

  useEffect(() => {
    // add a welcome message on initial load
    setMessages([
      {
        role: 'assistant',
        content:
          '**Welcome to CodeScan AI Assist!** Get instant solutions, explore CodeScan features, and access knowledge base articles & best practices. **👉 Type your question below to get started.**',
      },
    ]);
  }, []);

  const toggleMaximize = () => setIsMaximized((v) => !v); // Toggle maximized state

  function enforceBullets(text: string): string {
    return text
      .split(/\n+/) // split into lines
      .map((line) => line.trim())
      .filter(Boolean)
      .map((line) => {
        // If it looks like a heading with ":" → keep bold
        if (line.endsWith(':')) {
          console.log('Heading Line:', line);
          return `- ${line}`;
        }
        console.log('Line:', line);
        return `  ${line}`; // indent subtext under bullet
      })
      .join('\n');
  }

  const sendMessage = async () => {
    const text = input.trim();
    if (!text || sending) return;

    const userMsg: Message = { role: 'user', content: text };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setSending(true); // Set sending to true while waiting for a response

    try {
      const json = await postRequest<ChatbotAPIResponse, { query: string }>(API_URL, {
        query: text,
      });

      const replyText = json?.answer ?? json?.content ?? json?.response ?? 'No response.';
      // const formatted = enforceBullets(replyText);
      setMessages((prev) => [...prev, { role: 'assistant', content: replyText }]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: '⚠️ Error contacting chatbot service.' },
      ]);
    } finally {
      setSending(false); // Reset sending state after response
    }
  };

  const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {
    if (e.key === 'Enter') sendMessage();
  };

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isOpen]);

  
  function cleanText(s: string) {
    return s.replace(/\{\/?cs\}/gi, '').trim();
  }
  
  function formatStepsWithSubBullets(text: string): string {
    const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    const out: string[] = [];
  
    for (const line of lines) {
      if (/^\d+\./.test(line)) {
        // Numbered main step (e.g., "1. Do this")
        out.push(line);
      } else if (/^[-*]\s+/.test(line)) {
        // Already a bullet from backend → keep it
        out.push(line);
      } else if (/^[A-Z].*:/.test(line)) {
        // Looks like a heading with ":" (e.g., "Benefits:")
        out.push(`**${line}**`);
      } else {
        // Plain descriptive text → indent under previous step
        out.push(`- ${line}`);
      }
    }
  
    return out.join('\n');
  }  
  
  
  
  

  return (
    <div className={`chat-widget-container ${isMaximized ? 'maximized' : ''}`}>
      {isOpen ? (
        <div className={`chat-box ${isMaximized ? 'maximized' : ''}`}>
          <div className="chat-header">
            <div className="chat-title-wrapper">
              {/* <img src="/images/codescan-icon.svg" className="chat-logo" /> */}
              <span className="chat-logo-text">
                {'{'}
                <span className="chat-slash">/</span>cs{'}'}
              </span>
              <span className="chat-title"> CodeScan Assist</span>
            </div>
            <div className="chat-header-buttons">
              {/* Maximize/Minimize Button */}
              <button
                className="chat-maximize-btn"
                onClick={toggleMaximize}
                aria-label={isMaximized ? 'Minimize chat' : 'Maximize chat'}
              >
                <img
                  src={isMaximized ? '/images/window-minimize.svg' : '/images/window-maximize.svg'}
                  alt={isMaximized ? 'Minimize chat' : 'Maximize chat'}
                  className="maximize-icon"
                />
              </button>
              {/* Close Button */}
              <button className="chat-close-btn" onClick={toggleChat} aria-label="Close chat">
                <img src="/images/cross-icon.svg" alt="Close chat" className="close-icon" />
              </button>
            </div>
          </div>

          <div className="chat-body">
            {messages.map((m, i) => (
              <div key={i} className={`chat-bubble ${m.role === 'user' ? 'user' : 'bot'}`}>
                <img
                  className="avatar"
                  src={m.role === 'user' ? '/images/user-circle.svg' : '/images/codescan-icon.svg'}
                  alt={m.role}
                />

                {m.role === 'assistant' ? (
                  // ✅ Assistant: wrap markdown in a bubble
                  <div className="bubble-text">
                    <ReactMarkdown>{formatStepsWithSubBullets(cleanText(m.content))}</ReactMarkdown>
                  </div>
                ) : (
                  // ✅ User: simple bubble
                  <div className="bubble-text">{m.content}</div>
                )}
              </div>
            ))}

            {/* ✅ Typing/loading indicator (no nested bubble-text, no `m` here) */}
            {sending && (
              <div className="chat-bubble bot loading">
                <img className="avatar" src="/images/codescan-icon.svg" alt="assistant" />
                <div className="bubble-text">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            )}
          </div>

          <div className="chat-input-bar">
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={onKeyDown}
              placeholder={sending ? 'Sending…' : 'Ask me about CodeScan...'}
              disabled={sending}
            />
            <button className="send-btn" onClick={sendMessage} disabled={sending} aria-label="Send">
              send
            </button>
          </div>
        </div>
      ) : (
        <button className="chat-toggle-btn" onClick={toggleChat} aria-label="Open chat">
          <span>Chat</span>
        </button>
      )}
    </div>
  );
};

export default ChatWidget;
