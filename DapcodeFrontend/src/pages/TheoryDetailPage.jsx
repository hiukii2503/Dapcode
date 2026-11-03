import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import Navbar from '../components/Navbar';
import MarkdownRenderer from '../components/MarkdownRenderer';
import { getTheoryBySlug, getTheories } from '../api/theories';
import {
  ArrowLeft,
  BookOpen,
  ChevronLeft,
  ChevronRight,
  Code2,
  ListTree,
  ExternalLink
} from 'lucide-react';

const navBtnCls = 'btn btn-sm btn-ghost fs-xs d-inline-flex align-items-center gap-1 px-2';

export default function TheoryDetailPage() {
  const { slug } = useParams();
  const navigate = useNavigate();

  const [theory, setTheory] = useState(null);
  const [allTheories, setAllTheories] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  // Load current theory
  useEffect(() => {
    setIsLoading(true);
    setErrorMsg(null);

    getTheoryBySlug(slug)
      .then((res) => {
        const data = res?.data || res;
        if (data && data.slug) {
          setTheory(data);
        } else {
          setErrorMsg('Không tìm thấy nội dung bài giảng.');
        }
      })
      .catch((err) => {
        console.error('Lỗi khi tải chi tiết lý thuyết:', err);
        setErrorMsg('Không thể kết nối tới máy chủ backend.');
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, [slug]);

  // Load all theories for navigation sidebar
  useEffect(() => {
    getTheories()
      .then((res) => {
        const list = res?.data || res || [];
        if (Array.isArray(list)) {
          setAllTheories(list);
        }
      })
      .catch((err) => {
        console.error('Lỗi khi tải danh sách bài học:', err);
      });
  }, []);

  // Prev and next theory navigation
  const currentIndex = allTheories.findIndex((t) => t.slug === slug);
  const prevTheory = currentIndex > 0 ? allTheories[currentIndex - 1] : null;
  const nextTheory = currentIndex >= 0 && currentIndex < allTheories.length - 1 ? allTheories[currentIndex + 1] : null;

  if (isLoading) {
    return (
      <div className="d-flex flex-column min-vh-100 bg-canvas">
        <Navbar />
        <div className="flex-grow-1 d-flex flex-column align-items-center justify-content-center gap-3 text-body-secondary">
          <Spinner animation="border" size="sm" />
          <span className="fs-xs">Đang tải bài giảng lý thuyết...</span>
        </div>
      </div>
    );
  }

  if (errorMsg || !theory) {
    return (
      <div className="d-flex flex-column min-vh-100 bg-canvas">
        <Navbar />
        <div className="flex-grow-1 d-flex flex-column align-items-center justify-content-center gap-3 p-3">
          <BookOpen size={32} className="text-danger" />
          <h2 className="fs-base fw-semibold text-body-emphasis mb-0">Không tìm thấy bài học</h2>
          <p className="fs-xs text-body-secondary mb-0">{errorMsg || 'Bài học này không tồn tại trong hệ thống.'}</p>
          <Link to="/theories" className="btn btn-sm btn-contrast fs-xs px-3">
            Quay lại thư viện Lý thuyết
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="d-flex flex-column min-vh-100 bg-canvas">
      <Navbar />

      <main className="flex-grow-1 mx-auto w-100 px-3 px-sm-4 py-4" style={{ maxWidth: 1280 }}>
        <div className="row g-4 align-items-start">
          {/* Left Sidebar: Table of Contents & Syllabus Navigation (4 cols) */}
          <aside className="col-lg-4 d-none d-lg-block sticky-lg-top" style={{ top: 80 }}>
            <div className="card p-3 overflow-y-auto" style={{ maxHeight: '85vh' }}>
              <div className="d-flex align-items-center justify-content-between border-bottom pb-3 mb-3">
                <Link to="/theories" className="d-flex align-items-center gap-2 fs-xs fw-medium link-body-emphasis text-decoration-none">
                  <ArrowLeft size={14} />
                  <span>Tất cả bài học</span>
                </Link>
                <span className="fs-2xs font-monospace text-body-secondary">
                  {allTheories.length} bài
                </span>
              </div>

              <div className="fs-xs fw-semibold text-uppercase mb-2 d-flex align-items-center gap-2">
                <ListTree size={14} />
                <span>Chương trình giảng dạy</span>
              </div>

              <div className="d-flex flex-column gap-1">
                {allTheories.map((item) => {
                  const isActive = item.slug === slug;
                  return (
                    <Link
                      key={item.slug || item.id}
                      to={`/theories/${item.slug}`}
                      className={`d-flex align-items-start gap-2 p-2 rounded-2 fs-xs text-decoration-none ${
                        isActive
                          ? 'bg-body-tertiary text-body-emphasis fw-semibold border'
                          : 'text-body-secondary hover-bg'
                      }`}
                    >
                      <span className="fs-2xs font-monospace text-body-secondary">
                        #{item.orderIndex || 1}
                      </span>
                      <div className="min-w-0 flex-grow-1">
                        <div className="text-truncate">{item.title}</div>
                        <div className="fs-2xs fw-normal text-body-secondary">{item.category}</div>
                      </div>
                    </Link>
                  );
                })}
              </div>
            </div>
          </aside>

          {/* Right Main Article Reader (8 cols) */}
          <article className="col-12 col-lg-8">
            <div className="card p-4">
              {/* Breadcrumb & Navigation */}
              <div className="d-flex align-items-center justify-content-between border-bottom pb-3 mb-4">
                <div className="d-flex align-items-center gap-2 fs-xs">
                  <Link to="/theories" className="text-body-secondary text-decoration-none">
                    Lý thuyết
                  </Link>
                  <span className="text-body-tertiary">/</span>
                  <span className="fw-medium">{theory.category}</span>
                </div>

                <div className="d-flex align-items-center gap-2">
                  <button
                    type="button"
                    disabled={!prevTheory}
                    onClick={() => prevTheory && navigate(`/theories/${prevTheory.slug}`)}
                    className={navBtnCls}
                    title={prevTheory?.title}
                  >
                    <ChevronLeft size={14} />
                    <span className="d-none d-sm-inline">Bài trước</span>
                  </button>
                  <button
                    type="button"
                    disabled={!nextTheory}
                    onClick={() => nextTheory && navigate(`/theories/${nextTheory.slug}`)}
                    className={navBtnCls}
                    title={nextTheory?.title}
                  >
                    <span className="d-none d-sm-inline">Bài tiếp</span>
                    <ChevronRight size={14} />
                  </button>
                </div>
              </div>

              {/* Article Title & Meta */}
              <div className="mb-5">
                <div className="d-flex align-items-center gap-2 mb-2">
                  <span className="badge rounded-1 bg-body-tertiary border fs-xs text-body">
                    {theory.category}
                  </span>
                  <span className="fs-xs text-body-secondary font-monospace">
                    Bài học #{theory.orderIndex || 1}
                  </span>
                </div>
                <h1 className="fs-2xl fw-semibold text-body-emphasis mb-0">
                  {theory.title}
                </h1>
                {theory.summary && (
                  <p className="mt-3 mb-0 text-body-secondary fst-italic border-start border-2 border-secondary ps-3">
                    {theory.summary}
                  </p>
                )}
              </div>

              {/* Markdown Article Content */}
              <MarkdownRenderer content={theory.contentMarkdown} />

              {/* Practice CTA Footer: Flat Warm Minimalist Panel */}
              <div className="mt-5 rounded-2 border bg-body-tertiary p-4 d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
                <div>
                  <h4 className="fs-sm fw-semibold text-body-emphasis d-flex align-items-center gap-2 mb-0">
                    <Code2 size={16} />
                    <span>Áp dụng ngay vào thực hành</span>
                  </h4>
                  <p className="fs-xs text-body-secondary mt-1 mb-0">
                    Luyện tập các bài toán thuộc chủ đề {theory.category} trên NeetCode 150 Roadmap.
                  </p>
                </div>
                <Link
                  to="/roadmap"
                  className="btn btn-sm btn-contrast fs-xs d-inline-flex align-items-center gap-2 px-3 align-self-start align-self-sm-auto"
                >
                  <span>Mở Roadmap</span>
                  <ExternalLink size={14} />
                </Link>
              </div>
            </div>
          </article>
        </div>
      </main>
    </div>
  );
}
