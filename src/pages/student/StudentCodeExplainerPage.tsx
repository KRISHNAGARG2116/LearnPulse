import React, { useState } from 'react';
import { explainCode } from '../../api/lmsApi';
import { CodeExplainerResponse } from '../../types/lms';
import {
  Code,
  Terminal,
  Sparkles,
  Loader2,
  AlertCircle,
  Lightbulb,
  Cpu,
  Layers,
  ArrowRight,
  FileCode,
  Copy,
  Check
} from 'lucide-react';

const SAMPLE_SNIPPETS: Record<string, { label: string; language: string; code: string }> = {
  binarySearch: {
    label: 'Binary Search (Java)',
    language: 'java',
    code: `public int binarySearch(int[] arr, int target) {
    int low = 0;
    int high = arr.length - 1;

    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (arr[mid] == target) {
            return mid;
        } else if (arr[mid] < target) {
            low = mid + 1;
        } else {
            high = mid - 1;
        }
    }
    return -1;
}`,
  },
  quicksort: {
    label: 'QuickSort Partition (Python)',
    language: 'python',
    code: `def partition(arr, low, high):
    pivot = arr[high]
    i = low - 1
    for j in range(low, high):
        if arr[j] <= pivot:
            i += 1
            arr[i], arr[j] = arr[j], arr[i]
    arr[i + 1], arr[high] = arr[high], arr[i + 1]
    return i + 1`,
  },
  lruCache: {
    label: 'LRU Cache Node (C++)',
    language: 'cpp',
    code: `struct Node {
    int key;
    int value;
    Node* prev;
    Node* next;
    Node(int k, int v) : key(k), value(v), prev(nullptr), next(nullptr) {}
};`,
  },
};

export const StudentCodeExplainerPage: React.FC = () => {
  const [code, setCode] = useState<string>(SAMPLE_SNIPPETS.binarySearch.code);
  const [language, setLanguage] = useState<string>('java');
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [explanation, setExplanation] = useState<CodeExplainerResponse | null>(null);
  const [copied, setCopied] = useState<boolean>(false);

  const loadSample = (key: string) => {
    const sample = SAMPLE_SNIPPETS[key];
    if (sample) {
      setCode(sample.code);
      setLanguage(sample.language);
      setExplanation(null);
      setError(null);
    }
  };

  const handleCopyCode = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleExplain = async () => {
    if (!code.trim()) {
      setError('Source code is required and cannot be empty or blank.');
      return;
    }
    if (code.trim().length > 10000) {
      setError('Source code exceeds maximum allowed length of 10,000 characters.');
      return;
    }
    if (loading) return;

    setLoading(true);
    setError(null);

    try {
      const response = await explainCode({
        code: code.trim(),
        language: language || undefined,
      });

      setExplanation(response);
    } catch (err: any) {
      console.error('Code explanation error:', err);
      setError(
        err.response?.data?.message ||
        err.message ||
        'Failed to statically explain the code. Please try again.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-4 sm:p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-2xl backdrop-blur-md flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
            <Code className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-white">AI Static Code Explainer</h1>
              <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                Safe Static Analysis Only
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Deconstruct algorithm logic, complexity bounds, and variables without code execution.
            </p>
          </div>
        </div>

        {/* Preset Sample Quick Buttons */}
        <div className="flex items-center gap-2">
          <span className="text-[11px] text-slate-400 hidden sm:inline">Try preset:</span>
          {Object.entries(SAMPLE_SNIPPETS).map(([key, s]) => (
            <button
              key={key}
              onClick={() => loadSample(key)}
              className="px-2.5 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-[11px] font-medium border border-slate-700 transition"
            >
              {s.label.split(' ')[0]}
            </button>
          ))}
        </div>
      </div>

      {/* Editor & Controls */}
      <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-5 space-y-4 shadow-xl">
        <div className="flex items-center justify-between pb-2 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <Terminal className="w-4 h-4 text-cyan-400" />
            <span className="text-xs font-bold text-slate-200">Source Code Input</span>
          </div>

          <div className="flex items-center gap-3">
            {/* Language Selector */}
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              className="bg-slate-800 border border-slate-700 text-xs text-slate-200 rounded-lg px-2.5 py-1 focus:outline-none"
              aria-label="Code language"
            >
              <option value="java">Java</option>
              <option value="python">Python</option>
              <option value="cpp">C++</option>
              <option value="c">C</option>
              <option value="javascript">JavaScript</option>
              <option value="typescript">TypeScript</option>
              <option value="sql">SQL</option>
              <option value="go">Go</option>
              <option value="rust">Rust</option>
            </select>

            <button
              onClick={handleCopyCode}
              className="p-1 text-slate-400 hover:text-white transition"
              title="Copy code"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Code Input Textarea */}
        <div className="relative">
          <textarea
            value={code}
            onChange={(e) => setCode(e.target.value)}
            rows={10}
            placeholder="Paste your source code function, class, or algorithm snippet here..."
            className="w-full bg-slate-950 border border-slate-800 focus:border-cyan-500 rounded-xl p-4 font-mono text-xs sm:text-sm text-cyan-100 placeholder:text-slate-500 focus:outline-none leading-relaxed"
          />
        </div>

        {error && (
          <div className="p-3 rounded-xl bg-rose-950/60 border border-rose-800/60 text-xs text-rose-200 flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <button
          onClick={handleExplain}
          disabled={!code.trim() || loading}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-white font-bold text-xs sm:text-sm shadow-lg shadow-cyan-500/25 disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center gap-2 transition"
        >
          {loading ? (
            <>
              <Loader2 className="w-4 h-4 animate-spin" />
              Parsing AST & Synthesizing Static Explanation with Qwen AI...
            </>
          ) : (
            <>
              <Sparkles className="w-4 h-4" />
              Explain Code Architecture & Complexity
            </>
          )}
        </button>
      </div>

      {/* Structured Results Display */}
      {explanation && (
        <div className="space-y-4">
          {/* High-Level Purpose & Algorithm */}
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-800">
              <h3 className="text-xs font-bold text-cyan-400 uppercase tracking-wider flex items-center gap-1.5">
                <FileCode className="w-4 h-4" /> Functional Purpose & Algorithm Analysis
              </h3>
              {explanation.algorithm && (
                <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-cyan-950 text-cyan-300 border border-cyan-800/40">
                  Algorithm: {explanation.algorithm}
                </span>
              )}
            </div>
            <p className="text-sm text-slate-100 leading-relaxed font-medium">
              {explanation.purpose}
            </p>
            {explanation.controlFlow && (
              <div className="p-3 rounded-xl bg-slate-950/80 border border-slate-800/80 text-xs text-slate-300 space-y-1">
                <span className="font-semibold text-indigo-300">Control Flow & Execution Path:</span>
                <p className="leading-relaxed">{explanation.controlFlow}</p>
              </div>
            )}
          </div>

          {/* Complexity Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-400 flex items-center gap-1.5 uppercase">
                  <Cpu className="w-4 h-4 text-indigo-400" /> Time Complexity
                </span>
                <span className="px-2.5 py-0.5 rounded-full font-mono text-xs font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {explanation.timeComplexity}
                </span>
              </div>
              <p className="text-xs text-slate-300">
                Determined by loop bounds, recursive depth, and branch execution frequencies.
              </p>
            </div>

            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-400 flex items-center gap-1.5 uppercase">
                  <Layers className="w-4 h-4 text-purple-400" /> Space Complexity
                </span>
                <span className="px-2.5 py-0.5 rounded-full font-mono text-xs font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30">
                  {explanation.spaceComplexity}
                </span>
              </div>
              <p className="text-xs text-slate-300">
                Accounts for auxiliary memory allocation, recursion call frames, and data buffers.
              </p>
            </div>
          </div>

          {/* Step-by-Step Logic Breakdown */}
          {explanation.stepByStepLogic && explanation.stepByStepLogic.length > 0 && (
            <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 space-y-3">
              <h3 className="text-xs font-bold text-indigo-300 uppercase tracking-wider flex items-center gap-1.5">
                <ArrowRight className="w-4 h-4 text-indigo-400" /> Step-by-Step Execution Logic
              </h3>
              <div className="space-y-2.5">
                {explanation.stepByStepLogic.map((step, idx) => (
                  <div key={idx} className="flex items-start gap-3 text-xs text-slate-200">
                    <span className="w-6 h-6 rounded-lg bg-indigo-500/10 text-indigo-400 font-bold flex items-center justify-center shrink-0 border border-indigo-500/20 text-[11px]">
                      {idx + 1}
                    </span>
                    <span className="leading-relaxed pt-0.5">{step}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Variables and Methods Analysis */}
          {((explanation.variables && explanation.variables.length > 0) || (explanation.methods && explanation.methods.length > 0)) && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {explanation.variables && explanation.variables.length > 0 && (
                <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-2.5">
                  <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider">
                    Variables & State Roles
                  </h3>
                  <div className="space-y-1.5">
                    {explanation.variables.map((v, vIdx) => (
                      <div key={vIdx} className="p-2 rounded-lg bg-slate-950/80 border border-slate-800/80 text-xs text-slate-300 flex items-start gap-2">
                        <span className="w-1.5 h-1.5 rounded-full bg-cyan-400 mt-1.5 shrink-0" />
                        <span>{v}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {explanation.methods && explanation.methods.length > 0 && (
                <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 space-y-2.5">
                  <h3 className="text-xs font-bold text-slate-300 uppercase tracking-wider">
                    Functions & Methods
                  </h3>
                  <div className="space-y-1.5">
                    {explanation.methods.map((m, mIdx) => (
                      <div key={mIdx} className="p-2 rounded-lg bg-slate-950/80 border border-slate-800/80 text-xs text-slate-300 flex items-start gap-2">
                        <span className="w-1.5 h-1.5 rounded-full bg-purple-400 mt-1.5 shrink-0" />
                        <span>{m}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Potential Improvements & Edge Cases */}
          {explanation.potentialImprovements && explanation.potentialImprovements.length > 0 && (
            <div className="bg-slate-900/60 border border-amber-500/30 rounded-2xl p-5 space-y-3">
              <h3 className="text-xs font-bold text-amber-300 uppercase tracking-wider flex items-center gap-1.5">
                <Lightbulb className="w-4 h-4 text-amber-400" /> Potential Edge Cases & Architectural Improvements
              </h3>
              <ul className="space-y-2">
                {explanation.potentialImprovements.map((item, iIdx) => (
                  <li key={iIdx} className="text-xs text-slate-200 flex items-start gap-2">
                    <span className="w-1.5 h-1.5 rounded-full bg-amber-400 mt-1.5 shrink-0" />
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
