import React, { useEffect, useRef, useState } from 'react';
import '../styles/ChatWidget.css';

type Role = 'user' | 'assistant';
type Message = { role: Role; content: string };

const API_URL = 'http://localhost:3000/_codescan/chatbot/query';

const ChatWidget = () => {
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
      const reply = await sendToChatbot(text);
      setMessages((prev) => [...prev, { role: 'assistant', content: reply }]);
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
            <span className="chat-title">🧠 CODESCAN Assist</span>
            <button className="chat-close-btn" onClick={toggleChat} aria-label="Close chat">
              {/* <FaTimes /> */}close
            </button>
          </div>

          <div className="chat-body">
            {messages.map((m, i) => (
              <div key={i} className={`chat-bubble ${m.role === 'user' ? 'user' : 'bot'}`}>
                <img
                  className="avatar"
                  src={m.role === 'user' ? '/images/user-icon.png' : '/images/bot-icon.png'}
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
              placeholder={sending ? 'Sending…' : 'Type your message'}
              disabled={sending}
            />
            <button className="send-btn" onClick={sendMessage} disabled={sending} aria-label="Send">
              {/* <FaPaperPlane /> */}send
            </button>
          </div>
        </div>
      ) : (
        <button className="chat-toggle-btn" onClick={toggleChat} aria-label="Open chat">
          {/* <MdChat size={18} /> */}
          <span>Chat</span>
        </button>
      )}
    </div>
  );
};

async function sendToChatbot(query: string): Promise<string> {
  const res = await fetch(API_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query }),
  });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const json: any = await res.json();
  return json?.answer || json?.content || json?.response || 'No response.';
}

export default ChatWidget;
