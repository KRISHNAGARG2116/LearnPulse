import React, { useState, useEffect, useRef } from 'react';
import { sendAiChat, getAiChatHistory, getStudentEnrolledSubjects } from '../../api/lmsApi';
import { AiChatMessage, EnrolledSubjectDTO } from '../../types/lms';
import {
  Bot,
  User,
  Send,
  Loader2,
  RefreshCw,
  Sparkles,
  BookOpen,
  AlertCircle,
  PlusCircle
} from 'lucide-react';

export const StudentAiTutorPage: React.FC = () => {
  const [messages, setMessages] = useState<AiChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [conversationId, setConversationId] = useState<string>(() => 'conv_' + Date.now());
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [enrolledSubjects, setEnrolledSubjects] = useState<EnrolledSubjectDTO[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string>('');
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Suggested quick prompts
  const suggestedPrompts = [
    'Explain the OSI reference model and its 7 layers.',
    'What is the difference between TCP and UDP?',
    'Explain Dijkstra\'s shortest path algorithm step-by-step.',
    'How does process scheduling work in modern operating systems?',
  ];

  // Auto-scroll to bottom of conversation
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  // Load enrolled subjects and chat history on initial mount
  useEffect(() => {
    const fetchInitialData = async () => {
      try {
        const subjects = await getStudentEnrolledSubjects();
        setEnrolledSubjects(subjects || []);
        if (subjects && subjects.length > 0) {
          setSelectedSubject(subjects[0].subjectName);
        }
      } catch (e) {
        console.warn('Could not load enrolled subjects:', e);
      }

      try {
        const history = await getAiChatHistory();
        if (history && history.length > 0) {
          const formattedHistory: AiChatMessage[] = history.map((item) => ({
            id: item.id,
            role: item.role === 'ASSISTANT' ? 'assistant' : 'user',
            content: item.content,
            timestamp: new Date(item.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          }));
          setMessages(formattedHistory);
        } else {
          // Default welcome message
          setMessages([
            {
              id: 'welcome',
              role: 'assistant',
              content: '👋 Hello! I am your AI Academic Tutor powered by LearnPulse. Ask me anything about your course concepts, algorithms, code logic, or exam preparation!',
              timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            },
          ]);
        }
      } catch (e) {
        console.warn('Could not fetch prior chat history:', e);
        setMessages([
          {
            id: 'welcome',
            role: 'assistant',
            content: '👋 Hello! I am your AI Academic Tutor powered by LearnPulse. Ask me anything about your course concepts, algorithms, code logic, or exam preparation!',
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
          },
        ]);
      }
    };

    fetchInitialData();
  }, []);

  const handleSendMessage = async (customText?: string) => {
    const textToSend = customText || inputMessage;
    if (!textToSend.trim() || loading) return;

    const userMsg: AiChatMessage = {
      id: 'msg_' + Date.now(),
      role: 'user',
      content: textToSend.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputMessage('');
    setError(null);
    setLoading(true);

    try {
      const response = await sendAiChat(
        textToSend.trim(),
        conversationId,
        selectedSubject || undefined
      );

      const aiMsg: AiChatMessage = {
        id: 'msg_ai_' + Date.now(),
        role: 'assistant',
        content: response.aiResponse || 'No response generated.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, aiMsg]);
    } catch (err: any) {
      console.error('AI Tutor chat error:', err);
      const errorMsg =
        err.response?.data?.message ||
        err.message ||
        'The AI Tutor service is temporarily unavailable. Please try again.';
      setError(errorMsg);

      const errorAiMsg: AiChatMessage = {
        id: 'msg_err_' + Date.now(),
        role: 'assistant',
        content: `⚠️ **Service Notice**: ${errorMsg}`,
        error: true,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorAiMsg]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleStartNewChat = () => {
    setConversationId('conv_' + Date.now());
    setMessages([
      {
        id: 'welcome_new',
        role: 'assistant',
        content: '🌟 Started a fresh tutoring session! What topic or problem would you like to explore today?',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      },
    ]);
    setError(null);
  };

  // Helper to format basic markdown (bold, lists, code fences)
  const renderFormattedContent = (content: string) => {
    const lines = content.split('\n');
    let inCodeBlock = false;
    let codeBuffer: string[] = [];

    const elements: React.ReactNode[] = [];

    lines.forEach((line, idx) => {
      if (line.trim().startsWith('```')) {
        if (inCodeBlock) {
          // Close code block
          elements.push(
            <pre key={`code_${idx}`} className="my-2 p-3 bg-slate-950 text-indigo-200 rounded-lg text-xs font-mono overflow-x-auto border border-slate-800">
              <code>{codeBuffer.join('\n')}</code>
            </pre>
          );
          codeBuffer = [];
          inCodeBlock = false;
        } else {
          inCodeBlock = true;
        }
        return;
      }

      if (inCodeBlock) {
        codeBuffer.push(line);
        return;
      }

      // Check for bullet list
      if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
        const bulletText = line.trim().substring(2);
        elements.push(
          <li key={`li_${idx}`} className="ml-4 list-disc text-sm text-slate-200 my-0.5">
            {renderBoldSpan(bulletText)}
          </li>
        );
        return;
      }

      // Numbered list
      const numMatch = line.trim().match(/^(\d+)\.\s+(.*)/);
      if (numMatch) {
        elements.push(
          <div key={`num_${idx}`} className="ml-2 text-sm text-slate-200 my-0.5 flex gap-2">
            <span className="font-semibold text-indigo-400">{numMatch[1]}.</span>
            <span>{renderBoldSpan(numMatch[2])}</span>
          </div>
        );
        return;
      }

      // Regular paragraph or header
      if (line.trim().startsWith('### ')) {
        elements.push(
          <h4 key={`h3_${idx}`} className="text-sm font-bold text-indigo-300 mt-2 mb-1">
            {line.trim().substring(4)}
          </h4>
        );
      } else if (line.trim().startsWith('## ')) {
        elements.push(
          <h3 key={`h2_${idx}`} className="text-base font-bold text-indigo-200 mt-2 mb-1">
            {line.trim().substring(3)}
          </h3>
        );
      } else if (line.trim().length > 0) {
        elements.push(
          <p key={`p_${idx}`} className="text-sm text-slate-200 my-1 leading-relaxed">
            {renderBoldSpan(line)}
          </p>
        );
      } else {
        elements.push(<div key={`sp_${idx}`} className="h-1.5" />);
      }
    });

    if (inCodeBlock && codeBuffer.length > 0) {
      elements.push(
        <pre key="code_end" className="my-2 p-3 bg-slate-950 text-indigo-200 rounded-lg text-xs font-mono overflow-x-auto border border-slate-800">
          <code>{codeBuffer.join('\n')}</code>
        </pre>
      );
    }

    return elements;
  };

  const renderBoldSpan = (text: string) => {
    const parts = text.split(/(\*\*.*?\*\*)/g);
    return parts.map((part, i) => {
      if (part.startsWith('**') && part.endsWith('**')) {
        return <strong key={i} className="font-semibold text-white">{part.slice(2, -2)}</strong>;
      }
      return part;
    });
  };

  return (
    <div className="p-4 sm:p-6 max-w-6xl mx-auto space-y-4">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-900/60 border border-slate-800 p-4 rounded-2xl backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <Bot className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-white">LearnPulse AI Academic Tutor</h1>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                Qwen 2.5 Engine
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Interactive 24/7 academic pair-tutor for concepts, homework, and test preparation.
            </p>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2">
          {enrolledSubjects.length > 0 && (
            <div className="flex items-center gap-1.5 bg-slate-800/80 border border-slate-700/60 rounded-xl px-3 py-1.5">
              <BookOpen className="w-3.5 h-3.5 text-indigo-400" />
              <select
                value={selectedSubject}
                onChange={(e) => setSelectedSubject(e.target.value)}
                className="bg-transparent text-xs text-slate-200 focus:outline-none cursor-pointer"
                aria-label="Filter by subject"
              >
                <option value="" className="bg-slate-900 text-slate-200">General Academic</option>
                {enrolledSubjects.map((sub) => (
                  <option key={sub.subjectId} value={sub.subjectName} className="bg-slate-900 text-slate-200">
                    {sub.subjectName}
                  </option>
                ))}
              </select>
            </div>
          )}

          <button
            onClick={handleStartNewChat}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700/80 text-xs font-semibold text-slate-200 border border-slate-700/60 transition"
          >
            <PlusCircle className="w-3.5 h-3.5 text-purple-400" />
            New Chat
          </button>
        </div>
      </div>

      {/* Main Conversation Container */}
      <div className="bg-slate-900/40 border border-slate-800 rounded-2xl flex flex-col h-[650px] overflow-hidden shadow-xl">
        {/* Messages Scroll Area */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
          {messages.map((msg) => (
            <div
              key={msg.id}
              className={`flex gap-3 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              {msg.role === 'assistant' && (
                <div className="w-8 h-8 rounded-lg bg-indigo-600/30 border border-indigo-500/40 flex items-center justify-center shrink-0 mt-0.5">
                  <Bot className="w-4 h-4 text-indigo-300" />
                </div>
              )}

              <div
                className={`max-w-[85%] sm:max-w-[75%] rounded-2xl px-4 py-3 shadow-md ${
                  msg.role === 'user'
                    ? 'bg-indigo-600 text-white rounded-tr-none'
                    : msg.error
                    ? 'bg-rose-950/40 border border-rose-800/60 text-rose-200 rounded-tl-none'
                    : 'bg-slate-800/90 border border-slate-700/60 text-slate-100 rounded-tl-none'
                }`}
              >
                <div className="break-words">
                  {msg.role === 'assistant' ? renderFormattedContent(msg.content) : (
                    <p className="text-sm whitespace-pre-wrap">{msg.content}</p>
                  )}
                </div>
                {msg.timestamp && (
                  <div
                    className={`text-[10px] mt-1.5 ${
                      msg.role === 'user' ? 'text-indigo-200 text-right' : 'text-slate-400'
                    }`}
                  >
                    {msg.timestamp}
                  </div>
                )}
              </div>

              {msg.role === 'user' && (
                <div className="w-8 h-8 rounded-lg bg-purple-600/30 border border-purple-500/40 flex items-center justify-center shrink-0 mt-0.5">
                  <User className="w-4 h-4 text-purple-300" />
                </div>
              )}
            </div>
          ))}

          {/* Honest Loading State Indicator */}
          {loading && (
            <div className="flex gap-3 justify-start items-center">
              <div className="w-8 h-8 rounded-lg bg-indigo-600/30 border border-indigo-500/40 flex items-center justify-center shrink-0">
                <Bot className="w-4 h-4 text-indigo-300" />
              </div>
              <div className="bg-slate-800/80 border border-slate-700/60 rounded-2xl rounded-tl-none px-4 py-3 flex items-center gap-2.5">
                <Loader2 className="w-4 h-4 text-indigo-400 animate-spin" />
                <span className="text-xs text-slate-300 font-medium">
                  AI Tutor is analyzing your query with Qwen 2.5 Coder...
                </span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Quick Suggested Prompts (visible if conversation is short) */}
        {messages.length <= 2 && !loading && (
          <div className="px-4 py-2 bg-slate-900/80 border-t border-slate-800/60">
            <p className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider mb-1.5 flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-indigo-400" /> Suggested topics to ask:
            </p>
            <div className="flex flex-wrap gap-1.5">
              {suggestedPrompts.map((prompt, i) => (
                <button
                  key={i}
                  onClick={() => handleSendMessage(prompt)}
                  className="text-xs px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700/60 transition text-left"
                >
                  {prompt}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Error Notification Banner */}
        {error && (
          <div className="px-4 py-2 bg-rose-950/70 border-t border-rose-800/50 flex items-center justify-between gap-2 text-xs text-rose-200">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
              <span>{error}</span>
            </div>
            <button
              onClick={() => handleSendMessage()}
              className="flex items-center gap-1 px-2.5 py-1 rounded bg-rose-900/60 hover:bg-rose-800/80 text-[11px] font-semibold text-rose-100 transition"
            >
              <RefreshCw className="w-3 h-3" /> Retry
            </button>
          </div>
        )}

        {/* Input Bar */}
        <div className="p-3 bg-slate-900/90 border-t border-slate-800 flex items-end gap-2">
          <textarea
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={
              selectedSubject
                ? `Ask a question about ${selectedSubject}... (Enter to send, Shift+Enter for new line)`
                : 'Ask your academic question... (Enter to send, Shift+Enter for new line)'
            }
            rows={2}
            className="flex-1 bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-xl p-3 text-xs sm:text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none resize-none"
            disabled={loading}
          />
          <button
            onClick={() => handleSendMessage()}
            disabled={!inputMessage.trim() || loading}
            className="p-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-40 disabled:cursor-not-allowed text-white shadow-lg shadow-indigo-600/30 transition shrink-0"
            aria-label="Send message"
          >
            {loading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Send className="w-5 h-5" />}
          </button>
        </div>
      </div>
    </div>
  );
};
