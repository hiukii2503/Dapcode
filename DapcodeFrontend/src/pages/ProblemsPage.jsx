import React, { useState, useEffect, useMemo } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import { Search, CheckCircle2, Shuffle, X } from 'lucide-react';
import Navbar from '../components/Navbar';
import ProgressSidebar from '../components/ProgressSidebar';
import DifficultyBadge from '../components/DifficultyBadge';
import RoadmapGraph from '../components/RoadmapGraph';
import RoadmapTopicDrawer from '../components/RoadmapTopicDrawer';
import { getProblems } from '../api/problems';

const PAGE_SIZE = 50;

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
  const [page, setPage] = useState(1);
  const [selectedNode, setSelectedNode] = useState(null);

  const [completedSlugs, setCompletedSlugs] = useState(() => {
    try {
      return new Set(JSON.parse(localStorage.getItem('dapcode_completed') || '[]'));
    } catch {
      return new Set();
    }
  });

  const toggleComplete = (slug) => {
    setCompletedSlugs((prev) => {
      const next = new Set(prev);
      if (next.has(slug)) next.delete(slug);
      else next.add(slug);
      localStorage.setItem('dapcode_completed', JSON.stringify(Array.from(next)));
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
    setPage(1);
  }, [searchQuery, selectedDifficulty, selectedStatus, selectedTopic]);

  const topics = useMemo(() => {
    const counts = {};
    allProblems.forEach((p) => {
      const c = p.category || 'General';
      counts[c] = (counts[c] || 0) + 1;
    });
    return Object.entries(counts).sort((a, b) => b[1] - a[1]);
  }, [allProblems]);

  const filtered = useMemo(() => {
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

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const pageItems = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  const indexOf = (p) => allProblems.findIndex((x) => x.slug === p.slug) + 1;

  const pickRandom = () => {
    if (!filtered.length) return;
    const p = filtered[Math.floor(Math.random() * filtered.length)];
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
                <h1 className="fs-xl fw-bold mb-0">LiệtCode 150 Roadmap</h1>
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
                <button type="button" onClick={() => setSelectedTopic('ALL')} className={chipCls(selectedTopic === 'ALL')}>
                  All Topics
                </button>
                {topics.map(([name, count]) => (
                  <button
                    key={name}
                    type="button"
                    onClick={() => setSelectedTopic(name === selectedTopic ? 'ALL' : name)}
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
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Search questions"
                      className="form-control form-control-sm"
                      style={{ width: 224, paddingLeft: '2rem' }}
                    />
                  </div>

                  <select value={selectedDifficulty} onChange={(e) => setSelectedDifficulty(e.target.value)} className={selectCls}>
                    <option value="ALL">Difficulty</option>
                    <option value="EASY">Easy</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HARD">Hard</option>
                  </select>

                  <select value={selectedStatus} onChange={(e) => setSelectedStatus(e.target.value)} className={selectCls}>
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

                <div className="px-3 pb-1 text-body-secondary">
                  <span className="text-success fw-medium">{completedSlugs.size}</span>
                  <span> / {allProblems.length} Solved</span>
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
                  <tbody>
                    {pageItems.map((p) => {
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
                              <span className="text-body-secondary me-1">{indexOf(p)}.</span>
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
                    {pageItems.length === 0 && (
                      <tr>
                        <td colSpan={4} className="py-5 text-center text-body-secondary">
                          Không tìm thấy bài tập phù hợp.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>

                {totalPages > 1 && (
                  <div className="d-flex align-items-center justify-content-center gap-1 p-3 border-top">
                    {Array.from({ length: totalPages }, (_, i) => i + 1).map((n) => (
                      <button
                        key={n}
                        type="button"
                        onClick={() => setPage(n)}
                        className={n === page ? 'btn btn-sm btn-contrast px-2' : 'icon-btn w-auto px-2'}
                        style={{ minWidth: 32 }}
                      >
                        {n}
                      </button>
                    ))}
                  </div>
                )}
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
