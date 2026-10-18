import React, { useState, useEffect } from 'react';
import { generateFlashcards, getStudentEnrolledSubjects } from '../../api/lmsApi';
import { FlashcardItem, EnrolledSubjectDTO } from '../../types/lms';
import {
  Layers,
  RotateCw,
  CheckCircle2,
  XCircle,
  ChevronLeft,
  ChevronRight,
  Sparkles,
  Loader2,
  RefreshCw,
  Trophy,
  AlertCircle
} from 'lucide-react';

// Curriculum topic seed content used to dynamically invoke POST /api/ai/flashcards on demand
const CURRICULUM_SEEDS: Record<string, { title: string; seedText: string }> = {
  'os-concepts': {
    title: 'Operating Systems & Concurrency',
    seedText: `Operating systems manage hardware and provide common services for computer programs.
Key concepts:
1. Process vs Thread: A process is an executing program instance with its own virtual address space, file descriptors, and security context. A thread is an execution unit within a process that shares memory and heap with sibling threads.
2. Deadlocks: 4 Coffman conditions: Mutual Exclusion, Hold and Wait, No Preemption, Circular Wait. Deadlock prevention eliminates at least one condition. Banker's Algorithm prevents unsafe states.
3. Virtual Memory & Paging: Paging divides address space into fixed-size pages mapped to physical page frames via a Page Table. Page faults swap missing frames from disk.
4. Thrashing: Occurs when a system spends more execution cycles handling page faults and disk swapping than executing instructions.`,
  },
  'network-osi': {
    title: 'Computer Networks & OSI Model',
    seedText: `Computer networking connects autonomous computing devices.
Key concepts:
1. OSI 7-Layer Architecture: Physical (1), Data Link (2), Network (3), Transport (4), Session (5), Presentation (6), Application (7).
2. TCP 3-Way Handshake: SYN -> SYN-ACK -> ACK establishes reliable ordered byte-stream connection.
3. TCP vs UDP: TCP provides connection orientation, reliable delivery via sequence numbers and ACKs, flow control (sliding window), and congestion control. UDP provides connectionless, lightweight, low-latency datagram transmission.
4. Addressing: IP addresses (Layer 3) identify host networks; MAC addresses (Layer 2) identify physical network interfaces; ARP maps IP to MAC.`,
  },
  'dsa-algorithms': {
    title: 'Data Structures & Algorithms',
    seedText: `Algorithms and data structures form the computational core of software engineering.
Key concepts:
1. QuickSort: Divide-and-conquer sorting algorithm. Average time complexity is O(n log n); worst-case is O(n^2) when pivot selection is naive on sorted arrays.
2. Hash Tables & Collisions: Direct addressing with hash function. Collisions resolved by Separate Chaining (linked lists/red-black trees) or Open Addressing (linear/quadratic probing, double hashing).
3. Dijkstra's Algorithm: Single-source shortest path on weighted graphs with non-negative edge weights using a Min-Priority Queue in O((V + E) log V) time.`,
  },
};

export const StudentFlashcardsPage: React.FC = () => {
  const [activeDeckKey, setActiveDeckKey] = useState<string>('os-concepts');
  const [deckTitle, setDeckTitle] = useState<string>(CURRICULUM_SEEDS['os-concepts'].title);
  const [cards, setCards] = useState<FlashcardItem[]>([]);
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [isFlipped, setIsFlipped] = useState<boolean>(false);

  // Enrolled subjects for custom deck generation
  const [enrolledSubjects, setEnrolledSubjects] = useState<EnrolledSubjectDTO[]>([]);

  // Cache for dynamically generated decks
  const [cachedDecks, setCachedDecks] = useState<Record<string, FlashcardItem[]>>({});

  // Loading & Error States
  const [loadingDeck, setLoadingDeck] = useState<boolean>(false);
  const [deckError, setDeckError] = useState<string | null>(null);

  // Mastery Tracking: id -> 'known' | 'review' (In-memory Active Recall session)
  const [mastery, setMastery] = useState<Record<string, 'known' | 'review'>>({});
  const [isSessionCompleted, setIsSessionCompleted] = useState<boolean>(false);

  // Custom AI Flashcard Generator Modal Form
  const [isGeneratorOpen, setIsGeneratorOpen] = useState<boolean>(false);
  const [customContent, setCustomContent] = useState<string>('');
  const [customCardCount, setCustomCardCount] = useState<number>(5);
  const [generating, setGenerating] = useState<boolean>(false);
  const [generatorError, setGeneratorError] = useState<string | null>(null);

  // Fetch enrolled subjects on mount
  useEffect(() => {
    const fetchEnrolled = async () => {
      try {
        const subs = await getStudentEnrolledSubjects();
        setEnrolledSubjects(subs || []);
      } catch (err) {
        console.warn('Could not fetch enrolled subjects for flashcards:', err);
      }
    };
    fetchEnrolled();
  }, []);

  // Fetch or load dynamic flashcards from backend for the selected topic
  const loadDeckDynamically = async (key: string, forceRegenerate = false) => {
    setActiveDeckKey(key);
    setCurrentIndex(0);
    setIsFlipped(false);
    setMastery({});
    setIsSessionCompleted(false);
    setDeckError(null);

    // If already cached and not forced to regenerate, use cache
    if (!forceRegenerate && cachedDecks[key] && cachedDecks[key].length > 0) {
      setCards(cachedDecks[key]);
      return;
    }

    const seed = CURRICULUM_SEEDS[key];
    const content = seed ? seed.seedText : customContent;
    if (!content) return;

    setLoadingDeck(true);
    try {
      const response = await generateFlashcards({
        content,
        cardCount: 5,
      });

      if (response && response.flashcards && response.flashcards.length > 0) {
        const fetchedCards: FlashcardItem[] = response.flashcards.map((f, i) => ({
          id: `${key}-${Date.now()}-${i}`,
          question: f.question,
          answer: f.answer,
        }));

        setCards(fetchedCards);
        setCachedDecks((prev) => ({ ...prev, [key]: fetchedCards }));
      } else {
        setDeckError('The backend AI service returned an empty flashcard list.');
      }
    } catch (err: any) {
      console.error('Failed to load flashcards from backend:', err);
      setDeckError(
        err.response?.data?.message ||
        err.message ||
        'Failed to dynamically fetch flashcards from backend AI service. Please try again.'
      );
    } finally {
      setLoadingDeck(false);
    }
  };

  // Initial load on mount
  useEffect(() => {
    loadDeckDynamically('os-concepts');
  }, []);

  // Keyboard navigation: Space = flip, ArrowLeft = prev, ArrowRight = next
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (['input', 'textarea'].includes((e.target as HTMLElement).tagName.toLowerCase())) {
        return;
      }
      if (e.code === 'Space') {
        e.preventDefault();
        setIsFlipped((prev) => !prev);
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        handleNext();
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        handlePrev();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [currentIndex, cards.length, isSessionCompleted]);

  const currentCard = cards[currentIndex];

  const handleNext = () => {
    if (currentIndex < cards.length - 1) {
      setCurrentIndex((prev) => prev + 1);
      setIsFlipped(false);
    } else {
      setIsSessionCompleted(true);
    }
  };

  const handlePrev = () => {
    if (currentIndex > 0) {
      setCurrentIndex((prev) => prev - 1);
      setIsFlipped(false);
    }
  };

  const markKnown = () => {
    if (!currentCard) return;
    setMastery((prev) => ({ ...prev, [currentCard.id]: 'known' }));
    handleNext();
  };

  const markReview = () => {
    if (!currentCard) return;
    setMastery((prev) => ({ ...prev, [currentCard.id]: 'review' }));
    handleNext();
  };

  const restartFullDeck = () => {
    setCurrentIndex(0);
    setIsFlipped(false);
    setMastery({});
    setIsSessionCompleted(false);
  };

  const reviewUnmasteredCards = () => {
    const unmastered = cards.filter((c) => mastery[c.id] === 'review');
    if (unmastered.length > 0) {
      setCards(unmastered);
      setCurrentIndex(0);
      setIsFlipped(false);
      setMastery({});
      setIsSessionCompleted(false);
    } else {
      restartFullDeck();
    }
  };

  const handleGenerateCustomDeck = async () => {
    if (!customContent.trim() || generating) return;

    setGenerating(true);
    setGeneratorError(null);

    try {
      const response = await generateFlashcards({
        content: customContent.trim(),
        cardCount: customCardCount,
      });

      if (response && response.flashcards && response.flashcards.length > 0) {
        const generatedItems: FlashcardItem[] = response.flashcards.map((f, i) => ({
          id: 'custom-' + Date.now() + '-' + i,
          question: f.question,
          answer: f.answer,
        }));

        const customKey = 'custom-' + Date.now();
        setCards(generatedItems);
        setCachedDecks((prev) => ({ ...prev, [customKey]: generatedItems }));
        setActiveDeckKey(customKey);
        setDeckTitle('Custom AI Generated Deck');
        setCurrentIndex(0);
        setIsFlipped(false);
        setMastery({});
        setIsSessionCompleted(false);
        setIsGeneratorOpen(false);
        setCustomContent('');
      } else {
        setGeneratorError('No flashcards could be parsed from the content.');
      }
    } catch (err: any) {
      console.error('Flashcard generation error:', err);
      setGeneratorError(
        err.response?.data?.message ||
        err.message ||
        'Failed to generate flashcards from content. Please check content length.'
      );
    } finally {
      setGenerating(false);
    }
  };

  // Stats calculation
  const knownCount = Object.values(mastery).filter((v) => v === 'known').length;
  const reviewCount = Object.values(mastery).filter((v) => v === 'review').length;
  const progressPercent = cards.length > 0 ? Math.round(((currentIndex + 1) / cards.length) * 100) : 0;
  const masteryPercent = cards.length > 0 ? Math.round((knownCount / cards.length) * 100) : 0;

  return (
    <div className="p-4 sm:p-6 max-w-5xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="bg-slate-900/60 border border-slate-800 p-4 rounded-2xl backdrop-blur-md flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-amber-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-amber-500/20">
            <Layers className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-lg font-bold text-white">Dynamic AI Flashcard Revision</h1>
              <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-amber-500/10 text-amber-400 border border-amber-500/20">
                Active Recall Mode
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Curriculum decks generated on-demand by Spring AI & Ollama with in-memory session mastery tracking.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => loadDeckDynamically(activeDeckKey, true)}
            disabled={loadingDeck}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-200 border border-slate-700 transition"
            title="Regenerate this deck with fresh AI questions"
          >
            <RefreshCw className={`w-3.5 h-3.5 text-indigo-400 ${loadingDeck ? 'animate-spin' : ''}`} />
            Regenerate Deck
          </button>
          <button
            onClick={() => setIsGeneratorOpen(!isGeneratorOpen)}
            className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-xs font-semibold text-white shadow-md shadow-indigo-600/30 transition"
          >
            <Sparkles className="w-3.5 h-3.5" />
            Custom Material Deck
          </button>
        </div>
      </div>

      {/* AI Custom Deck Drawer */}
      {isGeneratorOpen && (
        <div className="p-5 bg-slate-900/90 border border-indigo-500/40 rounded-2xl space-y-4 shadow-xl">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-400" />
              <h2 className="text-sm font-bold text-white">Synthesize Flashcards from Notes / Syllabus</h2>
            </div>
            <button
              onClick={() => setIsGeneratorOpen(false)}
              className="text-slate-400 hover:text-white text-xs font-semibold"
            >
              Cancel
            </button>
          </div>

          <p className="text-xs text-slate-300">
            Paste educational material, chapter notes, or algorithmic summaries. The backend Spring AI service generates structured Q&A cards via <code>POST /api/ai/flashcards</code>.
          </p>

          <textarea
            value={customContent}
            onChange={(e) => setCustomContent(e.target.value)}
            placeholder="Paste syllabus text or notes here..."
            rows={4}
            className="w-full bg-slate-950 border border-slate-800 focus:border-indigo-500 rounded-xl p-3 text-xs sm:text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none"
          />

          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="flex items-center gap-2 text-xs text-slate-300">
              <label>Cards to synthesize:</label>
              <select
                value={customCardCount}
                onChange={(e) => setCustomCardCount(Number(e.target.value))}
                className="bg-slate-800 border border-slate-700 text-slate-200 rounded-lg px-2 py-1 text-xs"
              >
                <option value={3}>3 Cards</option>
                <option value={5}>5 Cards</option>
                <option value={8}>8 Cards</option>
                <option value={10}>10 Cards</option>
              </select>
            </div>

            <button
              onClick={handleGenerateCustomDeck}
              disabled={!customContent.trim() || generating}
              className="px-4 py-2 rounded-xl bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-400 hover:to-purple-500 text-xs font-bold text-white disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2 transition"
            >
              {generating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
              Synthesize Deck
            </button>
          </div>

          {generatorError && (
            <p className="text-xs text-rose-400 bg-rose-950/40 border border-rose-800/40 p-2.5 rounded-lg">
              {generatorError}
            </p>
          )}
        </div>
      )}

      {/* Deck Selector Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {Object.entries(CURRICULUM_SEEDS).map(([key, deck]) => (
          <button
            key={key}
            onClick={() => {
              setDeckTitle(deck.title);
              loadDeckDynamically(key);
            }}
            disabled={loadingDeck}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition border ${
              activeDeckKey === key
                ? 'bg-indigo-600 text-white border-indigo-500 shadow-md shadow-indigo-600/30'
                : 'bg-slate-900/60 text-slate-300 border-slate-800 hover:bg-slate-800'
            }`}
          >
            {deck.title}
          </button>
        ))}

        {enrolledSubjects.map((sub) => {
          const subKey = 'sub-' + sub.subjectId;
          return (
            <button
              key={subKey}
              onClick={() => {
                setDeckTitle(sub.subjectName);
                if (!CURRICULUM_SEEDS[subKey]) {
                  CURRICULUM_SEEDS[subKey] = {
                    title: sub.subjectName,
                    seedText: `Subject: ${sub.subjectName} (${sub.subjectCode}). Comprehensive examination revision concepts, terminology, and key algorithms.`,
                  };
                }
                loadDeckDynamically(subKey);
              }}
              disabled={loadingDeck}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition border ${
                activeDeckKey === subKey
                  ? 'bg-indigo-600 text-white border-indigo-500 shadow-md shadow-indigo-600/30'
                  : 'bg-slate-900/60 text-slate-300 border-slate-800 hover:bg-slate-800'
              }`}
            >
              📚 {sub.subjectName}
            </button>
          );
        })}
      </div>

      {/* Loading Indicator for Backend Deck Generation */}
      {loadingDeck && (
        <div className="p-12 bg-slate-900/60 border border-slate-800 rounded-2xl flex flex-col items-center justify-center gap-3 text-center">
          <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
          <p className="text-sm font-semibold text-slate-200">
            Generating flashcard deck dynamically via Ollama / Spring AI...
          </p>
          <p className="text-xs text-slate-400 max-w-sm">
            Synthesizing key questions, terms, and explanations from curriculum syllabus content.
          </p>
        </div>
      )}

      {/* Error Banner */}
      {deckError && !loadingDeck && (
        <div className="p-4 rounded-2xl bg-rose-950/60 border border-rose-800/60 text-xs text-rose-200 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-rose-400 shrink-0" />
            <span>{deckError}</span>
          </div>
          <button
            onClick={() => loadDeckDynamically(activeDeckKey, true)}
            className="px-3 py-1.5 rounded-lg bg-rose-900/80 hover:bg-rose-800 text-xs font-semibold text-white transition shrink-0"
          >
            Retry
          </button>
        </div>
      )}

      {/* Main Flashcard Arena */}
      {!loadingDeck && !isSessionCompleted && currentCard && (
        <div className="space-y-4">
          {/* Progress and Stats Bar */}
          <div className="flex items-center justify-between text-xs text-slate-400 px-1">
            <span className="font-semibold text-slate-300">
              Card {currentIndex + 1} of {cards.length} ({deckTitle})
            </span>
            <div className="flex items-center gap-4">
              <span className="flex items-center gap-1 text-emerald-400">
                <CheckCircle2 className="w-3.5 h-3.5" /> {knownCount} Mastered
              </span>
              <span className="flex items-center gap-1 text-amber-400">
                <XCircle className="w-3.5 h-3.5" /> {reviewCount} Review
              </span>
            </div>
          </div>

          {/* Visual Progress Bar */}
          <div className="w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-indigo-500 to-purple-500 transition-all duration-300"
              style={{ width: `${progressPercent}%` }}
            />
          </div>

          {/* 3D Flashcard Flip Container */}
          <div
            onClick={() => setIsFlipped(!isFlipped)}
            className="cursor-pointer perspective-[1000px] select-none"
            title="Click or press Spacebar to flip card"
          >
            <div
              className={`relative w-full min-h-[320px] rounded-2xl p-8 flex flex-col justify-between transition-all duration-500 transform shadow-2xl border ${
                isFlipped
                  ? 'bg-gradient-to-br from-indigo-950/80 via-slate-900 to-purple-950/80 border-indigo-500/60 ring-2 ring-indigo-500/20'
                  : 'bg-gradient-to-br from-slate-900 via-slate-900/90 to-slate-950 border-slate-800 hover:border-slate-700'
              }`}
            >
              {/* Card Header Tag */}
              <div className="flex items-center justify-between">
                <span
                  className={`text-[10px] font-bold tracking-widest uppercase px-2.5 py-0.5 rounded-full ${
                    isFlipped
                      ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                      : 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                  }`}
                >
                  {isFlipped ? 'Concept / Answer' : 'Question / Term'}
                </span>
                <span className="text-[11px] text-slate-400 flex items-center gap-1">
                  <RotateCw className="w-3 h-3" /> Flip card (Space)
                </span>
              </div>

              {/* Card Main Body */}
              <div className="py-6 flex items-center justify-center text-center">
                <p
                  className={`leading-relaxed whitespace-pre-wrap font-medium ${
                    isFlipped
                      ? 'text-base sm:text-lg text-indigo-100 font-sans'
                      : 'text-lg sm:text-xl text-white font-semibold'
                  }`}
                >
                  {isFlipped ? currentCard.answer : currentCard.question}
                </p>
              </div>

              {/* Card Footer Status */}
              <div className="flex items-center justify-between text-[11px] text-slate-400 pt-2 border-t border-slate-800/60">
                <span>
                  {mastery[currentCard.id] === 'known' ? (
                    <span className="text-emerald-400 font-semibold flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Marked as Mastered
                    </span>
                  ) : mastery[currentCard.id] === 'review' ? (
                    <span className="text-amber-400 font-semibold flex items-center gap-1">
                      <XCircle className="w-3.5 h-3.5" /> Marked for Review
                    </span>
                  ) : (
                    <span className="text-slate-400">Not yet rated</span>
                  )}
                </span>
                <span className="text-slate-400">Use arrow keys ← → to browse</span>
              </div>
            </div>
          </div>

          {/* Action Buttons: Know It vs Review Again & Navigation */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
            <button
              onClick={handlePrev}
              disabled={currentIndex === 0}
              className="px-4 py-3 rounded-xl bg-slate-900 border border-slate-800 hover:bg-slate-800 disabled:opacity-30 disabled:cursor-not-allowed text-xs font-semibold text-slate-200 flex items-center justify-center gap-1.5 transition"
            >
              <ChevronLeft className="w-4 h-4" /> Previous
            </button>

            <button
              onClick={markReview}
              className="px-4 py-3 rounded-xl bg-amber-500/10 border border-amber-500/30 hover:bg-amber-500/20 text-xs font-bold text-amber-300 flex items-center justify-center gap-1.5 transition shadow-sm"
            >
              <XCircle className="w-4 h-4 text-amber-400" /> Review Again
            </button>

            <button
              onClick={markKnown}
              className="px-4 py-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 hover:bg-emerald-500/20 text-xs font-bold text-emerald-300 flex items-center justify-center gap-1.5 transition shadow-sm"
            >
              <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Know It
            </button>

            <button
              onClick={handleNext}
              className="px-4 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-xs font-bold text-white flex items-center justify-center gap-1.5 transition shadow-md shadow-indigo-600/30"
            >
              Next <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Session Persistence Explanatory Note */}
          <p className="text-[11px] text-slate-500 text-center pt-2">
            💡 Active Recall Practice: Mastery is tracked in-memory for your active session. You can retry unmastered cards until 100% mastery is achieved.
          </p>
        </div>
      )}

      {/* Session Completed Summary View */}
      {!loadingDeck && isSessionCompleted && (
        <div className="p-8 bg-slate-900/60 border border-slate-800 rounded-2xl text-center space-y-6 backdrop-blur-md shadow-2xl">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-amber-400 to-indigo-600 flex items-center justify-center mx-auto shadow-lg shadow-indigo-500/25">
            <Trophy className="w-8 h-8 text-white" />
          </div>

          <div>
            <h2 className="text-xl font-bold text-white">Deck Revision Complete!</h2>
            <p className="text-xs text-slate-400 mt-1">
              You reviewed all {cards.length} cards in this study deck.
            </p>
          </div>

          {/* Score & Mastery Badges */}
          <div className="grid grid-cols-2 max-w-sm mx-auto gap-4">
            <div className="p-4 rounded-xl bg-emerald-950/40 border border-emerald-800/40 text-center">
              <span className="text-2xl font-black text-emerald-400">{knownCount}</span>
              <p className="text-xs text-emerald-300 font-semibold mt-0.5">Mastered ({masteryPercent}%)</p>
            </div>
            <div className="p-4 rounded-xl bg-amber-950/40 border border-amber-800/40 text-center">
              <span className="text-2xl font-black text-amber-400">{reviewCount}</span>
              <p className="text-xs text-amber-300 font-semibold mt-0.5">Needs Review</p>
            </div>
          </div>

          {/* Action choices */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2">
            {reviewCount > 0 && (
              <button
                onClick={reviewUnmasteredCards}
                className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-xs font-bold text-white shadow-md shadow-amber-600/20 transition flex items-center justify-center gap-2"
              >
                <RefreshCw className="w-4 h-4" />
                Review {reviewCount} Unmastered Cards
              </button>
            )}
            <button
              onClick={restartFullDeck}
              className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-bold text-slate-200 border border-slate-700 transition flex items-center justify-center gap-2"
            >
              <RotateCw className="w-4 h-4" />
              Restart Full Deck
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
