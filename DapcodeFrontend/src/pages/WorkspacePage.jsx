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
import { getStarterCode } from '../data/starterTemplates';
import { useTheme } from '../context/ThemeContext';
import { FEATURES } from '../config/features';
import { Spinner } from 'react-bootstrap';
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
  Moon
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

  // Modals state
  const [isAiTutorOpen, setIsAiTutorOpen] = useState(false);
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [isStreakOpen, setIsStreakOpen] = useState(false);

  // User state
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('dapcode_user') || 'null');
    } catch {
      return null;
    }
  });

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
    if (!FEATURES.aiTutor) return;
    if (!checkAuth('sử dụng trợ lý AI Tutor')) return;
    setIsAiTutorOpen(true);
  };

  // Run Code
  const handleRunCode = useCallback(async () => {
    if (!FEATURES.judge) return;
    if (!problem) return;
    if (!checkAuth('chạy thử code')) return;
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
    if (!FEATURES.judge) return;
    if (!problem) return;
    if (!checkAuth('nộp bài chấm điểm')) return;
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

      if (submissionData?.status?.toUpperCase() === 'ACCEPTED') {
        try {
          const completed = new Set(JSON.parse(localStorage.getItem('dapcode_completed') || '[]'));
          completed.add(problem.slug);
          localStorage.setItem('dapcode_completed', JSON.stringify(Array.from(completed)));
        } catch (e) {
          console.error(e);
        }
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
      <div className="d-flex vh-100 align-items-center justify-content-center text-body-secondary">
        <div className="d-flex flex-column align-items-center gap-3">
          <Spinner animation="border" size="sm" />
          <p className="fs-xs mb-0">Đang tải đề bài từ Database...</p>
        </div>
      </div>
    );
  }

  if (fetchError || !problem) {
    return (
      <div className="d-flex flex-column vh-100 align-items-center justify-content-center gap-3 p-3">
        <AlertCircle size={32} className="text-danger" />
        <h2 className="fs-base fw-semibold text-body-emphasis mb-0">Không thể tải bài tập</h2>
        <p className="fs-xs text-body-secondary text-center mb-0" style={{ maxWidth: 448 }}>
          {fetchError || 'Bài tập không tồn tại trong hệ thống.'}
        </p>
        <Link to="/roadmap" className="btn btn-sm btn-contrast fs-xs">
          Quay lại Roadmap
        </Link>
      </div>
    );
  }

  const aiTutorTitle = FEATURES.aiTutor ? undefined : 'Tính năng đang phát triển';

  return (
    <div className="d-flex flex-column vh-100 bg-canvas overflow-hidden">
      <header className="app-header d-flex align-items-center justify-content-between border-bottom bg-surface px-2 flex-shrink-0">
        <div className="d-flex align-items-center gap-2 min-w-0">
          <Link to="/roadmap" className="d-flex align-items-center pe-2" title="LiệtCode Roadmap">
            <DapcodeLogo size="sm" />
          </Link>

          <Link to="/practice" className="icon-btn w-auto px-2">
            <List size={16} />
            <span className="d-none d-sm-inline">Problem List</span>
          </Link>

          <div className="d-flex align-items-center">
            <button
              type="button"
              disabled={!prevProblem}
              onClick={() => prevProblem && navigate(`/problems/${prevProblem.slug}`)}
              className="icon-btn"
              title={prevProblem ? `Previous: ${prevProblem.title}` : 'No previous problem'}
            >
              <ChevronLeft size={16} />
            </button>
            <button
              type="button"
              disabled={!nextProblem}
              onClick={() => nextProblem && navigate(`/problems/${nextProblem.slug}`)}
              className="icon-btn"
              title={nextProblem ? `Next: ${nextProblem.title}` : 'No next problem'}
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>

        <div className="d-flex align-items-center gap-1">
          {/* Wrapper keeps the tooltip on a disabled .btn */}
          <span className="d-inline-block" title={aiTutorTitle}>
            <button
              type="button"
              onClick={handleOpenAiTutor}
              disabled={!FEATURES.aiTutor}
              className="btn btn-sm btn-soft-primary d-inline-flex align-items-center gap-2"
            >
              <Sparkles size={16} />
              <span className="d-none d-sm-inline">AI Tutor</span>
            </button>
          </span>

          <button
            type="button"
            onClick={() => setIsSearchOpen(true)}
            className="icon-btn"
            title="Tìm kiếm (Ctrl+K)"
          >
            <Search size={18} />
          </button>

          <button
            type="button"
            onClick={() => setIsStreakOpen(true)}
            className="icon-btn w-auto px-2"
            title="Chuỗi streak"
          >
            <Flame size={18} className="text-primary" />
            <span className="fw-medium">{user?.streakCount ?? user?.streak ?? 0}</span>
          </button>

          <button
            type="button"
            onClick={toggleTheme}
            className="icon-btn"
            title={isDark ? 'Chế độ sáng' : 'Chế độ tối'}
          >
            {isDark ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          {user ? (
            <div className="avatar ms-1">
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.username || 'User'} referrerPolicy="no-referrer" />
              ) : (
                (user.username || user.name || user.email || 'U').charAt(0).toUpperCase()
              )}
            </div>
          ) : (
            <button
              type="button"
              onClick={() => setIsAuthOpen(true)}
              className="icon-btn w-auto px-3 ms-1 fw-medium"
            >
              <User size={16} />
              <span>Sign In</span>
            </button>
          )}
        </div>
      </header>

      {/* Main Split-Pane Workspace */}
      <div className="flex-grow-1 min-h-0 p-2 overflow-hidden">
        <div className="row g-2 h-100">
          <div className="col-12 col-lg-5 h-100">
            <div className="bg-surface rounded-2 overflow-hidden d-flex flex-column h-100">
              <div className="d-flex align-items-center bg-body-tertiary border-bottom px-2 flex-shrink-0" style={{ height: 40 }}>
                <nav className="nav nav-pills nav-soft">
                  <button
                    type="button"
                    onClick={() => setActiveLeftTab('description')}
                    className={`nav-link ${activeLeftTab === 'description' ? 'active' : ''}`}
                  >
                    <FileText size={14} />
                    <span>Mô tả</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setActiveLeftTab('submissions')}
                    className={`nav-link ${activeLeftTab === 'submissions' ? 'active' : ''}`}
                  >
                    <History size={14} />
                    <span>Lịch sử nộp</span>
                  </button>
                </nav>
              </div>

              {/* Left Panel Content: Editorial Readable Style */}
              <div className="flex-grow-1 overflow-y-auto p-4">
                {activeLeftTab === 'description' ? (
                  <div className="d-flex flex-column gap-3">
                    <h1 className="fs-xl fw-semibold text-body-emphasis mb-0">
                      {currentIndex >= 0 ? `${currentIndex + 1}. ` : ''}
                      {problem.title}
                    </h1>
                    <div className="d-flex flex-wrap align-items-center gap-2 fs-xs text-body-secondary">
                      <DifficultyBadge difficulty={problem.difficulty} />
                      <span className="badge rounded-pill bg-body-secondary text-body-secondary px-3">
                        {problem.category}
                      </span>
                      <div className="d-flex align-items-center gap-1">
                        <Clock size={14} />
                        <span>{problem.timeLimitMs || 1000} ms</span>
                      </div>
                      <div className="d-flex align-items-center gap-1">
                        <Cpu size={14} />
                        <span>{((problem.memoryLimitKb || 262144) / 1024).toFixed(0)} MB</span>
                      </div>
                    </div>

                    {/* Markdown Description */}
                    <MarkdownRenderer content={problem.descriptionMarkdown} />
                  </div>
                ) : (
                  /* Submissions History */
                  <div className="d-flex flex-column gap-3">
                    <div className="fs-xs fw-medium text-body-secondary">Lịch sử bài nộp:</div>
                    <div className="rounded-2 border bg-body-tertiary p-3 fs-xs text-body-secondary">
                      Chưa có lượt nộp bài nào được ghi nhận cho bài toán này.
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Right Pane: Monaco Editor + Console Drawer (7 Cols on LG), split 60/40 */}
          <div className="col-12 col-lg-7 h-100 d-flex flex-column gap-2">
            <div className="w-100 min-h-0" style={{ flex: '3 1 0' }}>
              <CodeEditor
                code={code}
                onChange={setCode}
                language={language}
                onLanguageChange={handleLanguageChange}
                onResetCode={handleResetCode}
                onOpenAiTutor={handleOpenAiTutor}
              />
            </div>

            <div className="w-100 min-h-0" style={{ flex: '2 1 0' }}>
              <ConsoleDrawer
                publicTestCases={problem.publicTestCases || []}
                submissionState={submissionState}
                onRunCode={handleRunCode}
                onSubmitCode={handleSubmitCode}
              />
            </div>
          </div>
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
