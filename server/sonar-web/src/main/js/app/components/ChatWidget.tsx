/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import React, { useEffect, useRef, useState } from 'react';
import ReactMarkdown from 'react-markdown';
import { postChatRequest } from '../../api/codescan';
import '../styles/ChatWidget.css';

type Role = 'user' | 'assistant';
type Message = { role: Role; content: string };

type ChatbotAPIResponse = {
  answer?: string;
  content?: string;
  response?: string;
};

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
      setIsMaximized(false); // Reset maximized state when closing
    }
    setIsOpen((prev) => {
      const next = !prev;
      localStorage.setItem('chatIsOpen', String(next)); // persist state
      return next;
    });
  };

  useEffect(() => {
    const saved = localStorage.getItem('chatIsOpen');
    if (saved) {
      setIsOpen(saved === 'true');
    }
  }, []);

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

  const sendMessage = async () => {
    const text = input.trim();
    if (!text || sending) return;

    const userMsg: Message = { role: 'user', content: text };
    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setSending(true); // Set sending to true while waiting for a response

    try {
      const json = await postChatRequest<ChatbotAPIResponse, { query: string }>(API_URL, {
        query: text,
      });

      const replyText = json?.answer ?? json?.content ?? json?.response ?? 'No response.';
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
    if (isOpen && endRef.current) {
      endRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [messages, isOpen, isMaximized]);

  return (
    <div className={`chat-widget-container ${isMaximized ? 'maximized' : ''}`}>
      {isOpen ? (
        <div className={`chat-box ${isMaximized ? 'maximized' : ''}`}>
          <div className="chat-header">
            <div className="chat-title-wrapper">
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
                  <div className="bubble-text">
                    <ReactMarkdown
                      components={{
                        h1: ({ children }) => (
                          <h1 className="text-lg font-bold my-2">{children}</h1>
                        ),
                        h2: ({ children }) => (
                          <h2 className="text-base font-semibold my-2">{children}</h2>
                        ),
                        h3: ({ children }) => <h3 className="font-medium my-1">{children}</h3>,
                        p: ({ children }) => <p className="my-1 leading-relaxed">{children}</p>,
                        ul: ({ children }) => <ul className="list-disc pl-5 my-1">{children}</ul>,
                        ol: ({ children }) => (
                          <ol className="list-decimal pl-5 my-1">{children}</ol>
                        ),
                        li: ({ children }) => <li className="my-0.5">{children}</li>,
                        code: ({ children }) => (
                          <code className="bg-gray-200 px-1 py-0.5 rounded text-xs">
                            {children}
                          </code>
                        ),
                        pre: ({ children }) => (
                          <pre className="bg-gray-200 p-2 rounded text-sm overflow-x-auto">
                            {children}
                          </pre>
                        ),
                      }}
                    >
                      {m.content}
                    </ReactMarkdown>
                  </div>
                ) : (
                  <div className="bubble-text">{m.content}</div>
                )}
              </div>
            ))}
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
              Send
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
