import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import Navbar from '../components/Navbar';
import PaginationBar from '../components/PaginationBar';
import { getTheoriesPaged, getTheoryCategories } from '../api/theories';
import {
  BookOpen,
  Search,
  ArrowRight
} from 'lucide-react';

const DEFAULT_PAGE_SIZE = 9;
const PAGE_SIZE_OPTIONS = [6, 9, 12, 18];

const pillCls = (active) =>
  `btn btn-sm fs-xs text-nowrap ${active ? 'btn-contrast' : 'btn-ghost bg-surface border text-body-secondary'}`;

export default function TheoriesPage() {
  const [theories, setTheories] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(DEFAULT_PAGE_SIZE);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [allCount, setAllCount] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    getTheoryCategories()
      .then((catRes) => {
        if (!isMounted) return;
        const cats = catRes?.data || catRes || [];
        if (Array.isArray(cats)) setCategories(cats);
      })
      .catch((err) => {
        console.error('Lỗi khi tải danh mục lý thuyết:', err);
      });
    return () => {
      isMounted = false;
    };
  }, []);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);

    getTheoriesPaged({
      page,
      size: pageSize,
      category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      search: searchQuery.trim() || undefined,
    })
      .then((res) => {
        if (!isMounted) return;
        const data = res?.data || res || {};
        const list = Array.isArray(data.content) ? data.content : Array.isArray(data) ? data : [];
        const total = typeof data.totalElements === 'number' ? data.totalElements : list.length;
        const pages = typeof data.totalPages === 'number' ? data.totalPages : Math.max(1, Math.ceil(total / pageSize));
        setTheories(list);
        setTotalElements(total);
        setTotalPages(Math.max(1, pages));
        if (selectedCategory === 'ALL' && !searchQuery.trim()) {
          setAllCount(total);
        }
      })
      .catch((err) => {
        console.error('Lỗi khi tải dữ liệu lý thuyết DSA:', err);
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [page, pageSize, selectedCategory, searchQuery]);

  const handlePageChange = (nextPage) => {
    setPage(nextPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handlePageSizeChange = (nextSize) => {
    setPageSize(nextSize);
    setPage(0);
  };

  return (
    <div className="d-flex flex-column min-vh-100 bg-canvas">
      <Navbar />

      <main className="flex-grow-1 mx-auto w-100 px-3 px-sm-4 py-5" style={{ maxWidth: 1280 }}>
        <div className="mb-5">
          <div className="d-flex flex-column flex-sm-row align-items-sm-center justify-content-sm-between gap-3">
            <div>
              <h1 className="fs-2xl fw-semibold text-body-emphasis mb-0 d-flex align-items-center gap-2">
                <BookOpen size={24} />
                <span>Theories</span>
                {(!isLoading || allCount !== null) && (
                  <span className="badge rounded-1 fs-xs font-monospace bg-success-subtle text-success-emphasis border border-success-subtle">
                    {allCount ?? totalElements} bài học
                  </span>
                )}
              </h1>
              <p className="mt-1 mb-0 text-body-secondary">
                Hệ thống giáo trình lý thuyết cấu trúc dữ liệu & giải thuật tinh gọn, giải thích bản chất kèm độ phức tạp không thời gian.
              </p>
            </div>
          </div>

          <div className="mt-4 d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
            <div className="position-relative flex-grow-1" style={{ maxWidth: 448 }}>
              <Search size={16} className="position-absolute top-50 start-0 translate-middle-y ms-3 text-body-secondary" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setPage(0);
                }}
                placeholder="Tìm bài học lý thuyết (vd: Dynamic Array, Stack, Binary Search...)"
                className="form-control fs-xs bg-surface border"
                style={{ paddingLeft: '2.5rem' }}
              />
            </div>

            <div className="d-flex align-items-center gap-2 overflow-x-auto pb-1 pb-sm-0">
              <button
                type="button"
                onClick={() => {
                  setSelectedCategory('ALL');
                  setPage(0);
                }}
                className={pillCls(selectedCategory === 'ALL')}
              >
                Tất cả{allCount !== null ? ` (${allCount})` : ''}
              </button>
              {categories.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => {
                    setSelectedCategory(cat);
                    setPage(0);
                  }}
                  className={pillCls(selectedCategory === cat)}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        </div>

        {isLoading && theories.length === 0 ? (
          <div className="d-flex flex-column align-items-center justify-content-center gap-3 py-5 my-4 text-body-secondary">
            <Spinner animation="border" size="sm" />
            <span className="fs-xs">Đang tải danh sách bài học lý thuyết từ cơ sở dữ liệu...</span>
          </div>
        ) : theories.length > 0 ? (
          <>
            <div className={`row g-3 ${isLoading ? 'opacity-50' : ''}`}>
              {theories.map((theory) => (
                <div key={theory.slug || theory.id} className="col-12 col-md-6 col-lg-4">
                  <Link
                    to={`/theories/${theory.slug}`}
                    className="card h-100 justify-content-between p-4 text-body text-decoration-none hover-bg"
                  >
                    <div>
                      <div className="d-flex align-items-center justify-content-between mb-3">
                        <span className="badge rounded-1 bg-body-tertiary border fs-2xs text-body">
                          {theory.category}
                        </span>
                        <span className="fs-2xs font-monospace text-body-secondary">
                          Bài #{theory.orderIndex || 1}
                        </span>
                      </div>

                      <h3 className="fs-base fw-semibold text-body-emphasis mb-2">
                        {theory.title}
                      </h3>

                      <p
                        className="fs-xs text-body-secondary mb-3"
                        style={{ display: '-webkit-box', WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}
                      >
                        {theory.summary || 'Tìm hiểu cấu trúc, nguyên lý hoạt động và cài đặt chi tiết.'}
                      </p>
                    </div>

                    <div className="d-flex align-items-center justify-content-between pt-3 border-top fs-xs fw-medium text-body-secondary">
                      <span>Xem chi tiết</span>
                      <ArrowRight size={14} />
                    </div>
                  </Link>
                </div>
              ))}
            </div>

            <div className="card border-0 mt-4">
              <PaginationBar
                page={page}
                totalPages={totalPages}
                totalElements={totalElements}
                pageSize={pageSize}
                pageSizeOptions={PAGE_SIZE_OPTIONS}
                itemLabel="bài học"
                loading={isLoading}
                onPageChange={handlePageChange}
                onPageSizeChange={handlePageSizeChange}
              />
            </div>
          </>
        ) : (
          <div className="card p-5 text-center text-body-secondary">
            <p className="mb-0">Không tìm thấy bài học lý thuyết phù hợp với từ khóa.</p>
          </div>
        )}
      </main>
    </div>
  );
}
