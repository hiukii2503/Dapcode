import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import DifficultyBadge from '../components/DifficultyBadge';
import MarkdownRenderer from '../components/MarkdownRenderer';
import CodeEditor, { SUPPORTED_LANGUAGES } from '../components/CodeEditor';
import ConsoleDrawer from '../components/ConsoleDrawer';
import AiTutorDrawer from '../components/AiTutorDrawer';
import SearchModal from '../components/SearchModal';
import AuthModal from '../components/AuthModal';
import StreakModal from '../components/StreakModal';
import DapcodeLogo from '../components/DapcodeLogo';
import { getProblemBySlug, getProblems } from '../api/problems';
import { submitCode, subscribeSubmissionStream } from '../api/submissions';
import { getCurrentUser } from '../api/auth';
import { getStarterCode } from '../data/starterTemplates';
import { useTheme } from '../context/ThemeContext';
import { 
  ChevronLeft, 
  ChevronRight, 
  FileText, 
  History, 
  Sparkles, 
  Clock, 
  Cpu, 
  Search,
  Flame,
  User,
  List,
  AlertCircle,
  Sun,
  Moon,
  Play,
  CloudUpload,
  Shuffle,
  Terminal,
  Loader2,
  ChevronUp,
  LogOut,
  Shield
} from 'lucide-react';

export default function WorkspacePage() {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { toggleTheme, isDark } = useTheme();

  // Problem State
  const [problem, setProblem] = useState(null);
  const [allProblems, setAllProblems] = useState([]);
  const [isLoadingProblem, setIsLoadingProblem] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [activeLeftTab, setActiveLeftTab] = useState('description'); // 'description' | 'submissions'

  // Editor State
  const [language, setLanguage] = useState('python');
  const [code, setCode] = useState(() => getStarterCode(slug, 'python'));

  // Modals & Console Drawer state
  const [isAiTutorOpen, setIsAiTutorOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [isStreakOpen, setIsStreakOpen] = useState(false);
  const [isConsoleOpen, setIsConsoleOpen] = useState(false);
  const [consoleTab, setConsoleTab] = useState('testcases');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);

  // User state
  const [user, setUser] = useState(() => {
    try {
      const token = localStorage.getItem('dapcode_token');
      if (!token) {
        localStorage.removeItem('dapcode_user');
        return null;
      }
      return JSON.parse(localStorage.getItem('dapcode_user') || 'null');
    } catch {
      return null;
    }
  });

  useEffect(() => {
    const syncAuth = () => {
      try {
        const token = localStorage.getItem('dapcode_token');
        if (!token) {
          localStorage.removeItem('dapcode_user');
          setUser(null);
          return;
        }
        setUser(JSON.parse(localStorage.getItem('dapcode_user') || 'null'));
      } catch {
        setUser(null);
      }
    };
    window.addEventListener('dapcode-auth-change', syncAuth);

    const token = localStorage.getItem('dapcode_token');
    if (token) {
      getCurrentUser()
        .then((res) => {
          let userData = res?.data || res;
          if (userData && userData.data && typeof userData.data === 'object') {
            userData = userData.data;
          }
          if (userData && (userData.id || userData.email || userData.username)) {
            if (!userData.name && userData.username) userData.name = userData.username;
            localStorage.setItem('dapcode_user', JSON.stringify(userData));
            setUser(userData);
          }
        })
        .catch(() => {});
    }

    return () => window.removeEventListener('dapcode-auth-change', syncAuth);
  }, []);

  // Submission / Sandbox State
  const [submissionState, setSubmissionState] = useState({
    isRunning: false,
    isSubmitting: false,
    progress: null,
    result: null,
    error: null,
  });

  const [authPromptMessage, setAuthPromptMessage] = useState('');

  // Fetch full list of problems for dropdown selector and prev/next navigation
  useEffect(() => {
    getProblems({ size: 250 })
      .then((res) => {
        const items = res?.data?.content || res?.content || res?.data || [];
        if (Array.isArray(items)) {
          setAllProblems(items);
        }
      })
      .catch((err) => {
        console.error('Không thể tải danh sách bài tập:', err);
      });
  }, []);

  // Navigation: Prev & Next problems
  const currentIndex = allProblems.findIndex((p) => p.slug === slug);
  const prevProblem = currentIndex > 0 ? allProblems[currentIndex - 1] : null;
  const nextProblem = currentIndex >= 0 && currentIndex < allProblems.length - 1 ? allProblems[currentIndex + 1] : null;

  // Global Ctrl+K / Cmd+K listener
  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        setIsSearchOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Load problem details from Spring Boot Backend
  useEffect(() => {
    setIsLoadingProblem(true);
    setFetchError(null);
    setSubmissionState({
      isRunning: false,
      isSubmitting: false,
      progress: null,
      result: null,
      error: null,
    });

    getProblemBySlug(slug)
      .then((res) => {
        const data = res?.data || res;
        if (data && data.slug) {
          setProblem(data);
          const initialCode = data.starterCode?.[language] || getStarterCode(slug, language);
          setCode(initialCode);
        } else {
          setFetchError('Không tìm thấy dữ liệu bài toán từ backend.');
        }
      })
      .catch((err) => {
        console.error('Lỗi khi tải chi tiết bài tập:', err);
        const msg = err.response?.data?.message || err.message || 'Không thể kết nối tới máy chủ backend.';
        setFetchError(msg);
      })
      .finally(() => {
        setIsLoadingProblem(false);
      });
  }, [slug]);

  // Handle language switch
  const handleLanguageChange = (newLang) => {
    setLanguage(newLang);
    const newCode = problem?.starterCode?.[newLang] || getStarterCode(slug, newLang);
    setCode(newCode);
  };

  const handleResetCode = () => {
    const resetCode = problem?.starterCode?.[language] || getStarterCode(slug, language);
    setCode(resetCode);
  };

  const checkAuth = (actionName) => {
    const token = localStorage.getItem('dapcode_token');
    if (!token || !user) {
      setAuthPromptMessage(`Bạn cần đăng nhập tài khoản để ${actionName}, tính chuỗi Streak và nhận điểm XP!`);
      setIsAuthOpen(true);
      return false;
    }
    return true;
  };

  const handleOpenAiTutor = () => {
    if (!checkAuth('sử dụng trợ lý AI Tutor')) return;
    setIsAiTutorOpen(true);
  };

  // Run Code
  const handleRunCode = useCallback(async () => {
    if (!problem) return;
    if (!checkAuth('chạy thử code')) return;
    setIsConsoleOpen(true);
    setConsoleTab('result');
    setSubmissionState({
      isRunning: true,
      isSubmitting: false,
      progress: { message: 'Đang gửi mã nguồn tới Sandbox và kiểm thử các Test Case mẫu...' },
      result: null,
      error: null,
    });

    const langObj = SUPPORTED_LANGUAGES.find((l) => l.key === language) || SUPPORTED_LANGUAGES[0];

    try {
      const res = await submitCode({
        problemSlug: problem.slug,
        languageId: langObj.id,
        sourceCode: code,
      });

      const submissionData = res?.data || res;
      setSubmissionState({
        isRunning: false,
        isSubmitting: false,
        progress: null,
        result: submissionData,
        error: null,
      });

      if (submissionData?.submissionId) {
        subscribeSubmissionStream(submissionData.submissionId, {
          onProgress: (prog) => {
            setSubmissionState((prev) => ({
              ...prev,
              progress: prog,
            }));
          },
          onComplete: (completedRes) => {
            const mergedResult = {
              ...submissionData,
              ...completedRes,
              testCaseResults: (submissionData?.testCaseResults && submissionData.testCaseResults.length > 0)
                ? submissionData.testCaseResults
                : (completedRes?.testCaseResults || []),
            };
            setSubmissionState({
              isRunning: false,
              isSubmitting: false,
              progress: null,
              result: mergedResult,
              error: null,
            });
          },
          onError: () => {
            setSubmissionState((prev) => ({
              ...prev,
              isRunning: false,
              isSubmitting: false,
              progress: null,
              result: prev.result || submissionData,
              error: null,
            }));
          },
        });
      }
    } catch (err) {
      console.error('Sandbox Run API error:', err);
      const errMsg = err.response?.data?.message || err.message || 'Lỗi khi gửi bài tới Sandbox Judge0.';
      setSubmissionState({
        isRunning: false,
        isSubmitting: false,
        progress: null,
        result: null,
        error: errMsg,
      });
    }
  }, [problem, language, code, user]);

  // Submit Code
  const handleSubmitCode = useCallback(async () => {
    if (!problem) return;
    if (!checkAuth('nộp bài chấm điểm')) return;
    setIsConsoleOpen(true);
    setConsoleTab('result');
    setSubmissionState({
      isRunning: false,
      isSubmitting: true,
      progress: { message: 'Đang nộp bài và chấm trên bộ kiểm thử toàn diện...' },
      result: null,
      error: null,
    });

    const langObj = SUPPORTED_LANGUAGES.find((l) => l.key === language) || SUPPORTED_LANGUAGES[0];

    try {
      const res = await submitCode({
        problemSlug: problem.slug,
        languageId: langObj.id,
        sourceCode: code,
      });

      const submissionData = res?.data || res;
      setSubmissionState({
        isRunning: false,
        isSubmitting: false,
        progress: null,
        result: submissionData,
        error: null,
      });

      const markSolvedForCurrentUser = (solvedSlug) => {
        try {
          const uid = user?.id || user?.email || user?.username;
          if (!uid) return;
          const userKey = `dapcode_completed_${uid}`;
          const completed = new Set(JSON.parse(localStorage.getItem(userKey) || '[]'));
          completed.add(solvedSlug);
          localStorage.setItem(userKey, JSON.stringify(Array.from(completed)));
        } catch (e) {
          console.error(e);
        }
      };

      if (submissionData?.status?.toUpperCase() === 'ACCEPTED') {
        markSolvedForCurrentUser(problem.slug);
      }

      if (submissionData?.submissionId) {
        subscribeSubmissionStream(submissionData.submissionId, {
          onProgress: (prog) => {
            setSubmissionState((prev) => ({
              ...prev,
              progress: prog,
            }));
          },
          onComplete: (completedRes) => {
            const mergedResult = {
              ...submissionData,
              ...completedRes,
              testCaseResults: (submissionData?.testCaseResults && submissionData.testCaseResults.length > 0)
                ? submissionData.testCaseResults
                : (completedRes?.testCaseResults || []),
            };
            if (mergedResult?.status?.toUpperCase() === 'ACCEPTED') {
              markSolvedForCurrentUser(problem.slug);
            }
            setSubmissionState({
              isRunning: false,
              isSubmitting: false,
              progress: null,
              result: mergedResult,
              error: null,
            });
          },
          onError: () => {
            setSubmissionState((prev) => ({
              ...prev,
              isRunning: false,
              isSubmitting: false,
              progress: null,
              result: prev.result || submissionData,
              error: null,
            }));
          },
        });
      }
    } catch (err) {
      console.error('Sandbox Submit API error:', err);
      const errMsg = err.response?.data?.message || err.message || 'Lỗi khi gửi bài tới Sandbox Judge0.';
      setSubmissionState({
        isRunning: false,
        isSubmitting: false,
        progress: null,
        result: null,
        error: errMsg,
      });
    }
  }, [problem, language, code, user]);

  if (isLoadingProblem) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-[#FFFFFF] dark:bg-[#1A1A1A] text-neutral-600 dark:text-neutral-400 font-sans">
        <div className="flex flex-col items-center space-y-3">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-neutral-800 dark:border-neutral-200 border-t-transparent" />
          <p className="text-xs">Đang tải đề bài từ Database...</p>
        </div>
      </div>
    );
  }

  if (fetchError || !problem) {
    return (
      <div className="flex h-screen w-screen flex-col items-center justify-center bg-[#FFFFFF] dark:bg-[#1A1A1A] text-neutral-800 dark:text-neutral-200 font-sans p-4 space-y-4">
        <AlertCircle className="h-8 w-8 text-[#EF4743]" />
        <h2 className="text-base font-semibold text-[#262626] dark:text-[#EFF1F6]">Không thể tải bài tập</h2>
        <p className="text-xs text-neutral-600 dark:text-neutral-400 max-w-md text-center">{fetchError || 'Bài tập không tồn tại trong hệ thống.'}</p>
        <Link
          to="/roadmap"
          className="rounded-lg bg-[#262626] dark:bg-[#EFF1F6] px-4 py-2 text-xs font-medium text-[#F7F8FA] dark:text-[#262626] transition-colors"
        >
          Quay lại Roadmap
        </Link>
      </div>
    );
  }

  const isBusy = submissionState.isRunning || submissionState.isSubmitting;

  const handlePickRandom = () => {
    if (!allProblems.length) return;
    const pool = allProblems.filter((p) => p.slug !== slug);
    const list = pool.length > 0 ? pool : allProblems;
    const randomProblem = list[Math.floor(Math.random() * list.length)];
    if (randomProblem?.slug) {
      navigate(`/problems/${randomProblem.slug}`);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('dapcode_user');
    localStorage.removeItem('dapcode_token');
    localStorage.removeItem('dapcode_completed');
    setUser(null);
    setIsUserMenuOpen(false);
    window.dispatchEvent(new Event('dapcode-auth-change'));
  };

  return (
    <div className="flex h-screen flex-col bg-[#F0F0F0] dark:bg-[#1A1A1A] text-[#262626] dark:text-[#EFF1F6] overflow-hidden">
      <header className="relative flex h-[48px] items-center justify-between border-b border-[#E6E6E6] dark:border-[#3E3E3E] bg-white dark:bg-[#282828] px-3 flex-shrink-0">
        {/* Left Navigation Group */}
        <div className="flex items-center space-x-1 min-w-0">
          <Link to="/roadmap" className="flex items-center pr-1.5" title="LiệtCode Roadmap">
            <DapcodeLogo size="sm" />
          </Link>

          <div className="h-4 w-px bg-[#E6E6E6] dark:bg-[#3E3E3E]" />

          <Link
            to="/practice"
            className="flex items-center space-x-1.5 rounded-md px-2 py-1 text-xs font-medium text-neutral-700 dark:text-neutral-200 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] transition-colors"
          >
            <List className="h-4 w-4 text-neutral-500 dark:text-neutral-400" />
            <span className="hidden sm:inline">Problem List</span>
          </Link>

          <div className="flex items-center">
            <button
              type="button"
              disabled={!prevProblem}
              onClick={() => prevProblem && navigate(`/problems/${prevProblem.slug}`)}
              className="flex h-7 w-7 items-center justify-center rounded-md text-neutral-500 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] hover:text-[#262626] dark:hover:text-[#EFF1F6] disabled:opacity-30 transition-colors cursor-pointer"
              title={prevProblem ? `Bài trước: ${prevProblem.title}` : 'Không có bài trước'}
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              type="button"
              disabled={!nextProblem}
              onClick={() => nextProblem && navigate(`/problems/${nextProblem.slug}`)}
              className="flex h-7 w-7 items-center justify-center rounded-md text-neutral-500 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] hover:text-[#262626] dark:hover:text-[#EFF1F6] disabled:opacity-30 transition-colors cursor-pointer"
              title={nextProblem ? `Bài tiếp: ${nextProblem.title}` : 'Không có bài tiếp'}
            >
              <ChevronRight className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={handlePickRandom}
              className="flex h-7 w-7 items-center justify-center rounded-md text-neutral-500 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] hover:text-[#262626] dark:hover:text-[#EFF1F6] transition-colors cursor-pointer"
              title="Chọn bài ngẫu nhiên"
            >
              <Shuffle className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        {/* Center Action Group: Run | Submit | Testcase Toggle | AI Sparkles */}
        <div className="flex items-center space-x-1.5">
          <div className="flex items-center rounded-lg bg-[#F0F0F0] dark:bg-[#333333] p-0.5">
            <button
              type="button"
              disabled={isBusy}
              onClick={handleRunCode}
              title="Chạy thử code (Run)"
              className="flex h-7 items-center justify-center rounded-l-md px-2.5 text-neutral-600 dark:text-neutral-300 hover:bg-[#E2E2E2] dark:hover:bg-[#404040] disabled:opacity-50 transition-colors cursor-pointer"
            >
              {submissionState.isRunning ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin text-neutral-500" />
              ) : (
                <Play className="h-3.5 w-3.5 fill-current" />
              )}
            </button>

            <div className="h-4 w-px bg-[#DCDCDC] dark:bg-[#454545]" />

            <button
              type="button"
              disabled={isBusy}
              onClick={handleSubmitCode}
              title="Nộp bài chấm điểm (Submit)"
              className="flex h-7 items-center space-x-1.5 rounded-r-md px-3 text-xs font-semibold text-[#2CBB5D] hover:bg-[#E2E2E2] dark:hover:bg-[#404040] disabled:opacity-50 transition-colors cursor-pointer"
            >
              {submissionState.isSubmitting ? (
                <Loader2 className="h-3.5 w-3.5 animate-spin text-[#2CBB5D]" />
              ) : (
                <CloudUpload className="h-4 w-4 text-[#2CBB5D]" />
              )}
              <span>Submit</span>
            </button>
          </div>

          <button
            type="button"
            onClick={() => setIsConsoleOpen((v) => !v)}
            title={isConsoleOpen ? 'Ẩn bảng chấm Test Case' : 'Hiện bảng chấm Test Case'}
            className={`flex h-8 w-8 items-center justify-center rounded-lg transition-colors cursor-pointer ${
              isConsoleOpen
                ? 'bg-[#262626] text-white dark:bg-[#EFF1F6] dark:text-[#262626]'
                : 'bg-[#F0F0F0] dark:bg-[#333333] text-neutral-600 dark:text-neutral-300 hover:bg-[#E2E2E2] dark:hover:bg-[#404040]'
            }`}
          >
            <Terminal className="h-4 w-4" />
          </button>

          <button
            type="button"
            onClick={handleOpenAiTutor}
            title="Trợ lý AI Tutor"
            className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#F0F0F0] dark:bg-[#333333] text-[#7C3AED] dark:text-[#A78BFA] hover:bg-[#EDE9FE] dark:hover:bg-[#404040] transition-colors cursor-pointer"
          >
            <Sparkles className="h-4 w-4" />
          </button>
        </div>

        {/* Right Utility Group: Search, Theme, Streak, User Avatar */}
        <div className="flex items-center space-x-1">
          <button
            type="button"
            onClick={() => setIsSearchOpen(true)}
            className="flex h-8 w-8 items-center justify-center rounded-md text-neutral-500 dark:text-neutral-400 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] transition-colors cursor-pointer"
            title="Tìm kiếm bài tập (Ctrl+K)"
          >
            <Search className="h-4 w-4" />
          </button>

          <button
            type="button"
            onClick={toggleTheme}
            className="flex h-8 w-8 items-center justify-center rounded-md text-neutral-500 dark:text-neutral-400 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] transition-colors cursor-pointer"
            title={isDark ? 'Chế độ sáng' : 'Chế độ tối'}
          >
            {isDark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          <button
            type="button"
            onClick={() => setIsStreakOpen(true)}
            className="flex h-8 items-center space-x-1 rounded-md px-2 text-xs text-neutral-600 dark:text-neutral-300 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] transition-colors cursor-pointer"
            title="Chuỗi ngày học liên tiếp (Streak)"
          >
            <Flame className="h-4 w-4 text-[#FFA116]" />
            <span className="font-medium">{user?.streakCount ?? user?.streak ?? 0}</span>
          </button>

          {user ? (
            <div className="relative ml-1">
              <button
                type="button"
                onClick={() => setIsUserMenuOpen((v) => !v)}
                className="flex h-7 w-7 items-center justify-center rounded-full overflow-hidden bg-[#FFA116] text-white text-xs font-semibold cursor-pointer"
              >
                {user.avatarUrl ? (
                  <img
                    src={user.avatarUrl}
                    alt={user.username || 'User'}
                    referrerPolicy="no-referrer"
                    className="h-full w-full object-cover"
                  />
                ) : (
                  (user.username || user.name || user.email || 'U').charAt(0).toUpperCase()
                )}
              </button>

              {isUserMenuOpen && (
                <div className="absolute right-0 mt-2 w-52 rounded-lg border border-[#E6E6E6] dark:border-[#3E3E3E] bg-white dark:bg-[#282828] shadow-lg py-1 z-50">
                  <div className="px-3.5 py-2.5 border-b border-[#E6E6E6] dark:border-[#3E3E3E]">
                    <div className="text-xs font-semibold text-[#262626] dark:text-[#EFF1F6] truncate">
                      {user.username || user.name || 'User'}
                    </div>
                    <div className="text-[11px] text-neutral-500 dark:text-neutral-400 truncate">{user.email}</div>
                  </div>
                  <div className="flex items-center justify-between px-3.5 py-1.5 text-xs text-neutral-500 dark:text-neutral-400">
                    <span>XP</span>
                    <span className="font-medium text-[#262626] dark:text-[#EFF1F6]">{user.xp ?? 0}</span>
                  </div>
                  {user.role === 'ROLE_ADMIN' && (
                    <Link
                      to="/admin"
                      onClick={() => setIsUserMenuOpen(false)}
                      className="flex w-full items-center space-x-2 px-3.5 py-2 text-xs text-[#FFA116] hover:bg-[#FFA116]/10 font-medium"
                    >
                      <Shield className="h-3.5 w-3.5" />
                      <span>Trang Quản trị (Admin)</span>
                    </Link>
                  )}
                  <button
                    type="button"
                    onClick={handleLogout}
                    className="flex w-full items-center space-x-2 px-3.5 py-2 text-xs text-neutral-600 dark:text-neutral-300 hover:bg-[#F7F8FA] dark:hover:bg-[#333333] cursor-pointer"
                  >
                    <LogOut className="h-3.5 w-3.5" />
                    <span>Đăng xuất</span>
                  </button>
                </div>
              )}
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setIsAuthOpen(true)}
              className="ml-1 flex h-7 items-center space-x-1 rounded-md px-2.5 text-xs font-medium text-neutral-600 dark:text-neutral-300 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] transition-colors cursor-pointer"
            >
              <User className="h-3.5 w-3.5" />
              <span>Sign In</span>
            </button>
          )}
        </div>
      </header>

      {/* Main Split-Pane Workspace */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-12 gap-2 p-2 overflow-hidden">
        <div className="lg:col-span-5 flex flex-col rounded-lg bg-[#FFFFFF] dark:bg-[#282828] overflow-hidden h-full">
          <div className="flex h-10 items-center border-b border-[#E6E6E6] dark:border-[#3E3E3E] bg-[#F7F8FA] dark:bg-[#333333] px-2 space-x-1">
            <button
              type="button"
              onClick={() => setActiveLeftTab('description')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-md text-sm transition-colors cursor-pointer ${
                activeLeftTab === 'description'
                  ? 'text-[#262626] dark:text-[#EFF1F6] font-medium bg-white dark:bg-[#282828]'
                  : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-800 dark:hover:text-neutral-200'
              }`}
            >
              <FileText className="h-3.5 w-3.5" />
              <span>Mô tả</span>
            </button>

            <button
              type="button"
              onClick={() => setActiveLeftTab('submissions')}
              className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-md text-sm transition-colors cursor-pointer ${
                activeLeftTab === 'submissions'
                  ? 'text-[#262626] dark:text-[#EFF1F6] font-medium bg-white dark:bg-[#282828]'
                  : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-800 dark:hover:text-neutral-200'
              }`}
            >
              <History className="h-3.5 w-3.5" />
              <span>Lịch sử nộp</span>
            </button>
          </div>

          {/* Left Panel Content: Editorial Readable Style */}
          <div className="flex-1 overflow-y-auto p-5 text-sm">
            {activeLeftTab === 'description' ? (
              <div className="space-y-4">
                <h1 className="text-xl font-semibold text-[#262626] dark:text-[#EFF1F6]">
                  {currentIndex >= 0 ? `${currentIndex + 1}. ` : ''}
                  {problem.title}
                </h1>
                <div className="flex flex-wrap items-center gap-2 text-xs text-neutral-500 dark:text-neutral-400">
                  <DifficultyBadge difficulty={problem.difficulty} />
                  <span className="rounded-full bg-[#F0F0F0] dark:bg-[#333333] px-3 py-1 font-medium text-neutral-600 dark:text-neutral-300">
                    {problem.category}
                  </span>
                  <div className="flex items-center space-x-1">
                    <Clock className="h-3.5 w-3.5 text-neutral-400" />
                    <span>{problem.timeLimitMs || 1000} ms</span>
                  </div>
                  <div className="flex items-center space-x-1">
                    <Cpu className="h-3.5 w-3.5 text-neutral-400" />
                    <span>{((problem.memoryLimitKb || 262144) / 1024).toFixed(0)} MB</span>
                  </div>
                </div>

                {/* Markdown Description */}
                <MarkdownRenderer content={problem.descriptionMarkdown} />
              </div>
            ) : (
              /* Submissions History */
              <div className="space-y-3">
                <div className="text-xs font-medium text-neutral-600 dark:text-neutral-400 mb-2">Lịch sử bài nộp:</div>
                <div className="rounded-lg border border-[#E6E6E6] dark:border-[#3E3E3E] bg-[#F7F8FA] dark:bg-[#333333] p-3 text-xs text-neutral-500 dark:text-neutral-400">
                  Chưa có lượt nộp bài nào được ghi nhận cho bài toán này.
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Right Pane: Monaco Editor + Collapsible Console Drawer (7 Cols on LG) */}
        <div className="lg:col-span-7 flex flex-col h-full space-y-2 overflow-hidden">
          <div className={isConsoleOpen ? 'h-[56%] w-full min-h-0' : 'flex-1 w-full min-h-0'}>
            <CodeEditor
              code={code}
              onChange={setCode}
              language={language}
              onLanguageChange={handleLanguageChange}
              onResetCode={handleResetCode}
            />
          </div>

          {isConsoleOpen ? (
            <div className="h-[44%] w-full min-h-0 animate-in slide-in-from-bottom-2 duration-150">
              <ConsoleDrawer
                publicTestCases={problem.publicTestCases || []}
                submissionState={submissionState}
                activeTab={consoleTab}
                onTabChange={setConsoleTab}
                onClose={() => setIsConsoleOpen(false)}
              />
            </div>
          ) : (
            <div className="flex h-9 items-center justify-between rounded-lg bg-[#FFFFFF] dark:bg-[#282828] px-3 border border-[#E6E6E6] dark:border-[#3E3E3E] flex-shrink-0">
              <div className="flex items-center space-x-2">
                <button
                  type="button"
                  onClick={() => {
                    setConsoleTab('testcases');
                    setIsConsoleOpen(true);
                  }}
                  className="flex items-center space-x-1.5 rounded-md px-2.5 py-1 text-xs font-medium text-neutral-600 dark:text-neutral-300 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] transition-colors cursor-pointer"
                >
                  <Terminal className="h-3.5 w-3.5 text-[#2CBB5D]" />
                  <span>Test Cases</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setConsoleTab('result');
                    setIsConsoleOpen(true);
                  }}
                  className="flex items-center space-x-1.5 rounded-md px-2.5 py-1 text-xs font-medium text-neutral-600 dark:text-neutral-300 hover:bg-[#F0F0F0] dark:hover:bg-[#333333] transition-colors cursor-pointer"
                >
                  <span>Kết quả chấm</span>
                  {isBusy && <span className="h-1.5 w-1.5 rounded-full bg-[#2CBB5D] animate-pulse" />}
                </button>
              </div>
              <button
                type="button"
                onClick={() => setIsConsoleOpen(true)}
                className="flex items-center space-x-1 text-xs text-neutral-500 hover:text-[#262626] dark:hover:text-[#EFF1F6] cursor-pointer"
              >
                <span>Mở bảng</span>
                <ChevronUp className="h-3.5 w-3.5" />
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Socratic AI Tutor Drawer Flyout */}
      <AiTutorDrawer
        isOpen={isAiTutorOpen}
        onClose={() => setIsAiTutorOpen(false)}
        problemSlug={problem.slug}
        problemTitle={problem.title}
        currentCode={code}
        language={language}
        lastError={submissionState.error || (submissionState.result?.status !== 'ACCEPTED' ? submissionState.result?.status : '')}
      />

      {/* Global Modals in Workspace */}
      <SearchModal
        isOpen={isSearchOpen}
        onClose={() => setIsSearchOpen(false)}
        problems={allProblems}
      />
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        onLoginSuccess={(u) => setUser(u)}
        promptMessage={authPromptMessage}
      />
      <StreakModal
        isOpen={isStreakOpen}
        onClose={() => setIsStreakOpen(false)}
        streakCount={user?.streakCount ?? user?.streak ?? 1}
      />
    </div>
  );
}
