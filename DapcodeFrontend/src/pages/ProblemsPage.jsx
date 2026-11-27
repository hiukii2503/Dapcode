import React, { useState, useEffect, useMemo } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import { Search, CheckCircle2, Shuffle, X } from 'lucide-react';
import Navbar from '../components/Navbar';
import ProgressSidebar from '../components/ProgressSidebar';
import DifficultyBadge from '../components/DifficultyBadge';
import RoadmapGraph from '../components/RoadmapGraph';
import RoadmapTopicDrawer from '../components/RoadmapTopicDrawer';
import PaginationBar from '../components/PaginationBar';
import { getProblems } from '../api/problems';
import { getSolvedSlugs } from '../api/submissions';

const DEFAULT_PAGE_SIZE = 20;
const PAGE_SIZE_OPTIONS = [10, 20, 50];

const getUserCompletedKey = () => {
  try {
    const u = JSON.parse(localStorage.getItem('dapcode_user') || 'null');
    const uid = u?.id || u?.email || u?.username;
    return uid ? `dapcode_completed_${uid}` : null;
  } catch {
    return null;
  }
};

const selectCls = 'form-select form-select-sm w-auto cursor-pointer';

const chipCls = (active) =>
  `btn btn-sm rounded-pill flex-shrink-0 d-inline-flex align-items-center gap-2 px-3 ${
    active ? 'btn-contrast' : 'btn-ghost bg-surface text-body-secondary'
  }`;

export default function ProblemsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const isGraph = location.pathname === '/roadmap';

  const [allProblems, setAllProblems] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedDifficulty, setSelectedDifficulty] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');
  const [selectedTopic, setSelectedTopic] = useState('ALL');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [pagedData, setPagedData] = useState({
    content: [],
    totalElements: 0,
    totalPages: 1,
  });
  const [isPageLoading, setIsPageLoading] = useState(false);
  const [selectedNode, setSelectedNode] = useState(null);

  const [completedSlugs, setCompletedSlugs] = useState(new Set());

  const loadSolvedSlugs = () => {
    const token = localStorage.getItem('dapcode_token');
    const userKey = getUserCompletedKey();
    if (!token) {
      setCompletedSlugs(new Set());
      return;
    }

    getSolvedSlugs()
      .then((res) => {
        const slugs = Array.isArray(res?.data) ? res.data : Array.isArray(res) ? res : [];
        const merged = new Set(slugs);
        if (userKey) {
          localStorage.setItem(userKey, JSON.stringify(Array.from(merged)));
        }
        setCompletedSlugs(merged);
      })
      .catch(() => {
        if (userKey) {
          try {
            setCompletedSlugs(new Set(JSON.parse(localStorage.getItem(userKey) || '[]')));
            return;
          } catch {
          }
        }
        setCompletedSlugs(new Set());
      });
  };

  useEffect(() => {
    localStorage.removeItem('dapcode_completed');
    loadSolvedSlugs();

    const handleAuthChange = () => loadSolvedSlugs();
    window.addEventListener('dapcode-auth-change', handleAuthChange);
    return () => window.removeEventListener('dapcode-auth-change', handleAuthChange);
  }, []);

  const toggleComplete = (slug) => {
    const userKey = getUserCompletedKey();
    if (!userKey) return;
    setCompletedSlugs((prev) => {
      const next = new Set(prev);
      if (next.has(slug)) next.delete(slug);
      else next.add(slug);
      localStorage.setItem(userKey, JSON.stringify(Array.from(next)));
      return next;
    });
  };

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);
    getProblems({ size: 250 })
      .then((res) => {
        if (!isMounted) return;
        const items = res?.data?.content || res?.content || res?.data || [];
        if (Array.isArray(items)) setAllProblems(items);
      })
      .catch((err) => console.error('Không thể kết nối tới backend:', err))
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    if (isGraph || selectedStatus !== 'ALL') return;
    let isMounted = true;
    setIsPageLoading(true);
    getProblems({
      page,
      size: pageSize,
      category: selectedTopic !== 'ALL' ? selectedTopic : undefined,
      difficulty: selectedDifficulty !== 'ALL' ? selectedDifficulty : undefined,
      search: searchQuery.trim() || undefined,
    })
      .then((res) => {
        if (!isMounted) return;
        const data = res?.data || res || {};
        const content = Array.isArray(data.content) ? data.content : [];
        const totalElements = typeof data.totalElements === 'number' ? data.totalElements : content.length;
        const totalPages = typeof data.totalPages === 'number' ? data.totalPages : Math.max(1, Math.ceil(totalElements / pageSize));
        setPagedData({ content, totalElements, totalPages });
      })
      .catch((err) => console.error('Không thể tải trang bài tập:', err))
      .finally(() => {
        if (isMounted) setIsPageLoading(false);
      });
    return () => {
      isMounted = false;
    };
  }, [isGraph, selectedStatus, page, pageSize, selectedTopic, selectedDifficulty, searchQuery]);

  const topics = useMemo(() => {
    const counts = {};
    allProblems.forEach((p) => {
      const c = p.category || 'General';
      counts[c] = (counts[c] || 0) + 1;
    });
    return Object.entries(counts).sort((a, b) => b[1] - a[1]);
  }, [allProblems]);

  const statusFiltered = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return allProblems.filter((p) => {
      const matchQuery = !q || p.title?.toLowerCase().includes(q) || p.slug?.toLowerCase().includes(q);
      const matchDiff = selectedDifficulty === 'ALL' || p.difficulty?.toLowerCase() === selectedDifficulty.toLowerCase();
      const matchTopic = selectedTopic === 'ALL' || (p.category || 'General') === selectedTopic;
      const solved = completedSlugs.has(p.slug);
      const matchStatus =
        selectedStatus === 'ALL' || (selectedStatus === 'SOLVED' ? solved : !solved);
      return matchQuery && matchDiff && matchTopic && matchStatus;
    });
  }, [allProblems, searchQuery, selectedDifficulty, selectedTopic, selectedStatus, completedSlugs]);

  const useServerPaging = selectedStatus === 'ALL';
  const totalElements = useServerPaging ? pagedData.totalElements : statusFiltered.length;
  const totalPages = useServerPaging
    ? Math.max(1, pagedData.totalPages)
    : Math.max(1, Math.ceil(statusFiltered.length / pageSize));
  const pageItems = useServerPaging
    ? pagedData.content
    : statusFiltered.slice(page * pageSize, (page + 1) * pageSize);

  const indexOf = (p, idx) => {
    const found = allProblems.findIndex((x) => x.slug === p.slug);
    return found >= 0 ? found + 1 : page * pageSize + idx + 1;
  };

  const handlePageChange = (nextPage) => {
    setPage(nextPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handlePageSizeChange = (nextSize) => {
    setPageSize(nextSize);
    setPage(0);
  };

  const pickRandom = () => {
    const pool = statusFiltered.length ? statusFiltered : allProblems;
    if (!pool.length) return;
    const p = pool[Math.floor(Math.random() * pool.length)];
    navigate(`/problems/${p.slug}`);
  };

  const hasFilter =
    searchQuery || selectedDifficulty !== 'ALL' || selectedStatus !== 'ALL' || selectedTopic !== 'ALL';

  return (
    <div className="d-flex flex-column min-vh-100 bg-canvas">
      <Navbar />

      <main className="flex-grow-1 app-container mx-auto w-100 px-3 py-4">
        {isLoading ? (
          <div className="d-flex flex-column align-items-center justify-content-center gap-3 py-5 my-5 text-body-secondary">
            <Spinner animation="border" size="sm" />
            <span>Đang tải dữ liệu...</span>
          </div>
        ) : isGraph ? (
          <div className="d-flex flex-column gap-3">
            <div className="d-flex align-items-end justify-content-between">
              <div>
                <h1 className="fs-xl fw-bold mb-0">LiệtCode 250 Roadmap</h1>
                <p className="mt-1 mb-0 text-body-secondary">
                  Lộ trình học theo chủ đề — bấm vào một nút để xem danh sách bài tập.
                </p>
              </div>
              <span className="text-body-secondary">
                {completedSlugs.size}/{allProblems.length} Solved
              </span>
            </div>
            <RoadmapGraph
              problems={allProblems}
              completedSlugs={completedSlugs}
              onSelectNode={(node) => setSelectedNode(node)}
            />
          </div>
        ) : (
          <div className="row g-4 align-items-start">
            <div className="col-12 col-lg-8 col-xl-9 min-w-0 d-flex flex-column gap-3">
              <div className="d-flex align-items-center gap-2 overflow-x-auto pb-1">
                <button
                  type="button"
                  onClick={() => {
                    setSelectedTopic('ALL');
                    setPage(0);
                  }}
                  className={chipCls(selectedTopic === 'ALL')}
                >
                  All Topics
                </button>
                {topics.map(([name, count]) => (
                  <button
                    key={name}
                    type="button"
                    onClick={() => {
                      setSelectedTopic(name === selectedTopic ? 'ALL' : name);
                      setPage(0);
                    }}
                    className={chipCls(selectedTopic === name)}
                  >
                    <span>{name}</span>
                    <span className="fs-xs opacity-50">{count}</span>
                  </button>
                ))}
              </div>

              <div className="card border-0">
                <div className="d-flex flex-wrap align-items-center gap-2 p-3">
                  <div className="position-relative">
                    <Search size={16} className="position-absolute top-50 start-0 translate-middle-y ms-2 text-body-secondary" />
                    <input
                      type="text"
                      value={searchQuery}
                      onChange={(e) => {
                        setSearchQuery(e.target.value);
                        setPage(0);
                      }}
                      placeholder="Search questions"
                      className="form-control form-control-sm"
                      style={{ width: 224, paddingLeft: '2rem' }}
                    />
                  </div>

                  <select
                    value={selectedDifficulty}
                    onChange={(e) => {
                      setSelectedDifficulty(e.target.value);
                      setPage(0);
                    }}
                    className={selectCls}
                  >
                    <option value="ALL">Difficulty</option>
                    <option value="EASY">Easy</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HARD">Hard</option>
                  </select>

                  <select
                    value={selectedStatus}
                    onChange={(e) => {
                      setSelectedStatus(e.target.value);
                      setPage(0);
                    }}
                    className={selectCls}
                  >
                    <option value="ALL">Status</option>
                    <option value="TODO">Todo</option>
                    <option value="SOLVED">Solved</option>
                  </select>

                  {hasFilter && (
                    <button
                      type="button"
                      onClick={() => {
                        setSearchQuery('');
                        setSelectedDifficulty('ALL');
                        setSelectedStatus('ALL');
                        setSelectedTopic('ALL');
                        setPage(0);
                      }}
                      className="icon-btn w-auto px-2"
                    >
                      <X size={14} />
                      <span>Reset</span>
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={pickRandom}
                    className="btn btn-sm btn-ghost ms-auto d-inline-flex align-items-center gap-2"
                  >
                    <Shuffle size={16} />
                    <span>Pick One</span>
                  </button>
                </div>

                <div className="px-3 pb-1 d-flex align-items-center justify-content-between text-body-secondary">
                  <div>
                    <span className="text-success fw-medium">{completedSlugs.size}</span>
                    <span> / {allProblems.length} Solved</span>
                  </div>
                  {isPageLoading && useServerPaging && (
                    <span className="d-inline-flex align-items-center gap-1 fs-xs text-body-secondary">
                      <Spinner animation="border" size="sm" style={{ width: 12, height: 12 }} />
                      <span>Đang tải trang...</span>
                    </span>
                  )}
                </div>

                <table className="table table-striped table-hover align-middle mt-1 mb-0">
                  <thead>
                    <tr className="fs-xs">
                      <th className="ps-3 text-body-secondary" style={{ width: 56 }}>Status</th>
                      <th className="text-body-secondary">Title</th>
                      <th className="text-body-secondary d-none d-md-table-cell" style={{ width: 176 }}>Topic</th>
                      <th className="pe-3 text-body-secondary text-end" style={{ width: 112 }}>Difficulty</th>
                    </tr>
                  </thead>
                  <tbody className={isPageLoading && useServerPaging ? 'opacity-50' : ''}>
                    {pageItems.map((p, idx) => {
                      const solved = completedSlugs.has(p.slug);
                      return (
                        <tr key={p.slug || p.id}>
                          <td className="ps-3">
                            <button
                              type="button"
                              onClick={() => toggleComplete(p.slug)}
                              className="d-inline-flex border-0 bg-transparent p-0 cursor-pointer"
                              title={solved ? 'Solved' : 'Mark as solved'}
                            >
                              {solved ? (
                                <CheckCircle2 size={18} className="text-success" />
                              ) : (
                                <span className="d-block" style={{ width: 18, height: 18 }} />
                              )}
                            </button>
                          </td>
                          <td>
                            <Link to={`/problems/${p.slug}`} className="link-body-emphasis text-decoration-none">
                              <span className="text-body-secondary me-1">{indexOf(p, idx)}.</span>
                              {p.title}
                            </Link>
                          </td>
                          <td className="text-body-secondary d-none d-md-table-cell">{p.category}</td>
                          <td className="pe-3 text-end">
                            <DifficultyBadge difficulty={p.difficulty} variant="text" />
                          </td>
                        </tr>
                      );
                    })}
                    {!isPageLoading && pageItems.length === 0 && (
                      <tr>
                        <td colSpan={4} className="py-5 text-center text-body-secondary">
                          Không tìm thấy bài tập phù hợp.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>

                <PaginationBar
                  page={page}
                  totalPages={totalPages}
                  totalElements={totalElements}
                  pageSize={pageSize}
                  pageSizeOptions={PAGE_SIZE_OPTIONS}
                  itemLabel="bài tập"
                  loading={isPageLoading && useServerPaging}
                  onPageChange={handlePageChange}
                  onPageSizeChange={handlePageSizeChange}
                  className="border-top"
                />
              </div>
            </div>

            <aside className="col-12 col-lg-4 col-xl-3 sticky-lg-top" style={{ top: 66 }}>
              <ProgressSidebar problems={allProblems} completedSlugs={completedSlugs} />
            </aside>
          </div>
        )}
      </main>

      <RoadmapTopicDrawer
        node={selectedNode}
        isOpen={Boolean(selectedNode)}
        onClose={() => setSelectedNode(null)}
        problems={allProblems}
        completedSlugs={completedSlugs}
        onToggleComplete={toggleComplete}
      />
    </div>
  );
}
