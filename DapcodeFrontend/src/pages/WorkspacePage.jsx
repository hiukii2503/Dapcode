import React, { useState, useEffect, useCallback } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Dropdown, Spinner } from 'react-bootstrap';
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
  ChevronUp,
  LogOut,
  Shield,
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
    window.dispatchEvent(new Event('dapcode-auth-change'));
  };

  const displayName = user?.username || user?.name || user?.email?.split('@')[0] || 'User';
  const initialLetter = displayName.charAt(0).toUpperCase();

  return (
    <div className="d-flex flex-column vh-100 bg-canvas overflow-hidden">
      <header className="app-header d-flex align-items-center justify-content-between border-bottom bg-surface px-3 flex-shrink-0">
        {/* Left Navigation Group */}
        <div className="d-flex align-items-center gap-1 min-w-0">
          <Link to="/roadmap" className="d-flex align-items-center pe-2 text-decoration-none" title="LiệtCode Roadmap">
            <DapcodeLogo size="sm" />
          </Link>

          <div className="vr my-2 opacity-25" />

          <Link to="/practice" className="icon-btn w-auto px-2 fs-xs fw-medium text-decoration-none">
            <List size={16} />
            <span className="d-none d-sm-inline">Problem List</span>
          </Link>

          <div className="d-flex align-items-center">
            <button
              type="button"
              disabled={!prevProblem}
              onClick={() => prevProblem && navigate(`/problems/${prevProblem.slug}`)}
              className="icon-btn"
              title={prevProblem ? `Bài trước: ${prevProblem.title}` : 'Không có bài trước'}
            >
              <ChevronLeft size={16} />
            </button>
            <button
              type="button"
              disabled={!nextProblem}
              onClick={() => nextProblem && navigate(`/problems/${nextProblem.slug}`)}
              className="icon-btn"
              title={nextProblem ? `Bài tiếp: ${nextProblem.title}` : 'Không có bài tiếp'}
            >
              <ChevronRight size={16} />
            </button>
            <button
              type="button"
              onClick={handlePickRandom}
              className="icon-btn"
              title="Chọn bài ngẫu nhiên"
            >
              <Shuffle size={14} />
            </button>
          </div>
        </div>

        {/* Center Action Group: Run | Submit | Testcase Toggle | AI Sparkles */}
        <div className="d-flex align-items-center gap-2">
          <div className="d-inline-flex align-items-center rounded-2 bg-body-secondary p-1">
            <button
              type="button"
              disabled={isBusy}
              onClick={handleRunCode}
              title="Chạy thử code (Run)"
              className="btn btn-sm btn-ghost border-0 d-inline-flex align-items-center justify-content-center px-2 py-1"
            >
              {submissionState.isRunning ? (
                <Spinner animation="border" size="sm" className="text-body-secondary" />
              ) : (
                <Play size={14} fill="currentColor" />
              )}
            </button>

            <div className="vr opacity-25 my-1" />

            <button
              type="button"
              disabled={isBusy}
              onClick={handleSubmitCode}
              title="Nộp bài chấm điểm (Submit)"
              className="btn btn-sm btn-ghost border-0 d-inline-flex align-items-center gap-1 px-2 py-1 fs-xs fw-semibold text-success"
            >
              {submissionState.isSubmitting ? (
                <Spinner animation="border" size="sm" className="text-success" />
              ) : (
                <CloudUpload size={16} className="text-success" />
              )}
              <span>Submit</span>
            </button>
          </div>

          <button
            type="button"
            onClick={() => setIsConsoleOpen((v) => !v)}
            title={isConsoleOpen ? 'Ẩn bảng chấm Test Case' : 'Hiện bảng chấm Test Case'}
            className={`btn btn-sm d-inline-flex align-items-center justify-content-center p-0 rounded-2 ${
              isConsoleOpen ? 'btn-contrast' : 'btn-ghost'
            }`}
            style={{ width: 32, height: 32 }}
          >
            <Terminal size={16} />
          </button>

          <button
            type="button"
            onClick={handleOpenAiTutor}
            title="Trợ lý AI Tutor"
            className="btn btn-sm btn-soft-primary d-inline-flex align-items-center justify-content-center p-0 rounded-2"
            style={{ width: 32, height: 32 }}
          >
            <Sparkles size={16} />
          </button>
        </div>

        {/* Right Utility Group: Search, Theme, Streak, User Avatar */}
        <div className="d-flex align-items-center gap-1">
          <button
            type="button"
            onClick={() => setIsSearchOpen(true)}
            className="icon-btn"
            title="Tìm kiếm bài tập (Ctrl+K)"
          >
            <Search size={16} />
          </button>

          <button
            type="button"
            onClick={toggleTheme}
            className="icon-btn"
            title={isDark ? 'Chế độ sáng' : 'Chế độ tối'}
          >
            {isDark ? <Sun size={16} /> : <Moon size={16} />}
          </button>

          <button
            type="button"
            onClick={() => setIsStreakOpen(true)}
            className="icon-btn w-auto px-2 fs-xs"
            title="Chuỗi ngày học liên tiếp (Streak)"
          >
            <Flame size={16} className="text-primary" />
            <span className="fw-medium">{user?.streakCount ?? user?.streak ?? 0}</span>
          </button>

          {user ? (
            <Dropdown align="end" className="ms-1">
              <Dropdown.Toggle as="button" bsPrefix="avatar avatar-sm cursor-pointer" title={displayName}>
                {user.avatarUrl ? (
                  <img src={user.avatarUrl} alt={displayName} referrerPolicy="no-referrer" />
                ) : (
                  initialLetter
                )}
              </Dropdown.Toggle>

              <Dropdown.Menu className="shadow py-1">
                <div className="px-3 py-2 border-bottom">
                  <div className="fs-xs fw-semibold text-body-emphasis text-truncate">{displayName}</div>
                  <div className="fs-2xs text-body-secondary text-truncate">{user.email}</div>
                </div>
                <div className="d-flex align-items-center justify-content-between px-3 py-2 fs-xs text-body-secondary">
                  <span>XP</span>
                  <span className="fw-medium text-body-emphasis">{user.xp ?? 0}</span>
                </div>
                {user.role === 'ROLE_ADMIN' && (
                  <Dropdown.Item as={Link} to="/admin" className="d-flex align-items-center gap-2 fs-xs text-primary fw-medium">
                    <Shield size={14} />
                    <span>Trang Quản trị (Admin)</span>
                  </Dropdown.Item>
                )}
                <Dropdown.Item as="button" onClick={handleLogout} className="d-flex align-items-center gap-2 fs-xs text-body-secondary">
                  <LogOut size={14} />
                  <span>Đăng xuất</span>
                </Dropdown.Item>
              </Dropdown.Menu>
            </Dropdown>
          ) : (
            <button
              type="button"
              onClick={() => setIsAuthOpen(true)}
              className="icon-btn w-auto px-2 ms-1 fs-xs fw-medium"
            >
              <User size={14} />
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

          {/* Right Pane: Monaco Editor + Collapsible Console Drawer (7 Cols on LG) */}
          <div className="col-12 col-lg-7 h-100 d-flex flex-column gap-2 overflow-hidden">
            <div
              className="w-100 min-h-0"
              style={isConsoleOpen ? { flex: '14 1 0' } : { flex: '1 1 0' }}
            >
              <CodeEditor
                code={code}
                onChange={setCode}
                language={language}
                onLanguageChange={handleLanguageChange}
                onResetCode={handleResetCode}
              />
            </div>

            {isConsoleOpen ? (
              <div className="w-100 min-h-0" style={{ flex: '11 1 0' }}>
                <ConsoleDrawer
                  publicTestCases={problem.publicTestCases || []}
                  submissionState={submissionState}
                  activeTab={consoleTab}
                  onTabChange={setConsoleTab}
                  onClose={() => setIsConsoleOpen(false)}
                />
              </div>
            ) : (
              <div
                className="d-flex align-items-center justify-content-between rounded-2 bg-surface px-3 border flex-shrink-0"
                style={{ height: 36 }}
              >
                <div className="d-flex align-items-center gap-1">
                  <button
                    type="button"
                    onClick={() => {
                      setConsoleTab('testcases');
                      setIsConsoleOpen(true);
                    }}
                    className="icon-btn w-auto px-2 fs-xs fw-medium"
                  >
                    <Terminal size={14} className="text-success" />
                    <span>Test Cases</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setConsoleTab('result');
                      setIsConsoleOpen(true);
                    }}
                    className="icon-btn w-auto px-2 fs-xs fw-medium"
                  >
                    <span>Kết quả chấm</span>
                    {isBusy && <Spinner animation="grow" className="text-success ms-1" style={{ width: 6, height: 6 }} />}
                  </button>
                </div>
                <button
                  type="button"
                  onClick={() => setIsConsoleOpen(true)}
                  className="icon-btn w-auto px-2 fs-xs"
                >
                  <span>Mở bảng</span>
                  <ChevronUp size={14} />
                </button>
              </div>
            )}
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
        onLoginSuccess={(u) => {
          setUser(u);
          window.dispatchEvent(new Event('dapcode-auth-change'));
        }}
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
