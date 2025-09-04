import React, { useEffect, useRef, useState } from 'react';
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
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const endRef = useRef<HTMLDivElement | null>(null);

  const toggleChat = () => setIsOpen((v) => !v);

  const sendMessage = async () => {
    const text = input.trim();
    if (!text || sending) return;

    const userMsg: Message = { role: 'user', content: text };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setSending(true);

    try {
      // Pass BOTH generics: <ResponseType, BodyType>
      const json = await postRequest<ChatbotAPIResponse, { query: string }>(API_URL, {
        query: text, // ✅ use trimmed text, not (possibly cleared) input
      });

      const replyText = json?.answer ?? json?.content ?? json?.response ?? 'No response.';

      setMessages((prev) => [...prev, { role: 'assistant', content: replyText }]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: '⚠️ Error contacting chatbot service.' },
      ]);
    } finally {
      setSending(false);
    }
  };

  const onKeyDown: React.KeyboardEventHandler<HTMLInputElement> = (e) => {
    if (e.key === 'Enter') sendMessage();
  };

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isOpen]);

  return (
    <div className="chat-widget-container">
      {isOpen ? (
        <div className="chat-box">
          <div className="chat-header">
            <span className="chat-title">CODESCAN Assist</span>
            <button className="chat-close-btn" onClick={toggleChat} aria-label="Close chat">
              <img src="/images/Cross-icon.svg" alt="Close chat" className="close-icon" />
            </button>
          </div>

          <div className="chat-body">
            {messages.map((m, i) => (
              <div key={i} className={`chat-bubble ${m.role === 'user' ? 'user' : 'bot'}`}>
                <img
                  className="avatar"
                  src={
                    m.role === 'user' ? '/images/UserCircle-thin.svg' : '/images/codescan cs.svg'
                  }
                  alt={m.role}
                />
                <span className="bubble-text">{m.content}</span>
              </div>
            ))}
            <div ref={endRef} />
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
