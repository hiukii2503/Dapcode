import React, { useState, useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import Navbar from '../components/Navbar';
import { getTheories, getTheoryCategories } from '../api/theories';
import {
  BookOpen,
  Search,
  ArrowRight
} from 'lucide-react';

const pillCls = (active) =>
  `btn btn-sm fs-xs text-nowrap ${active ? 'btn-contrast' : 'btn-ghost bg-surface border text-body-secondary'}`;

export default function TheoriesPage() {
  const [theories, setTheories] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;
    setIsLoading(true);

    Promise.all([getTheories(), getTheoryCategories()])
      .then(([theoriesRes, catRes]) => {
        if (!isMounted) return;
        const list = theoriesRes?.data || theoriesRes || [];
        const cats = catRes?.data || catRes || [];
        if (Array.isArray(list)) setTheories(list);
        if (Array.isArray(cats)) setCategories(cats);
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
  }, []);

  const filteredTheories = useMemo(() => {
    return theories.filter((t) => {
      const matchCat = selectedCategory === 'ALL' || t.category === selectedCategory;
      const matchQuery =
        !searchQuery ||
        t.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.summary?.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.category?.toLowerCase().includes(searchQuery.toLowerCase());
      return matchCat && matchQuery;
    });
  }, [theories, selectedCategory, searchQuery]);

  return (
    <div className="d-flex flex-column min-vh-100 bg-canvas">
      <Navbar />

      <main className="flex-grow-1 mx-auto w-100 px-3 px-sm-4 py-5" style={{ maxWidth: 1280 }}>
        {/* Header Hero */}
        <div className="mb-5">
          <div className="d-flex flex-column flex-sm-row align-items-sm-center justify-content-sm-between gap-3">
            <div>
              <h1 className="fs-2xl fw-semibold text-body-emphasis mb-0 d-flex align-items-center gap-2">
                <BookOpen size={24} />
                <span>Theories</span>
                {!isLoading && (
                  <span className="badge rounded-1 fs-xs font-monospace bg-success-subtle text-success-emphasis border border-success-subtle">
                    {theories.length} bài học
                  </span>
                )}
              </h1>
              <p className="mt-1 mb-0 text-body-secondary">
                Hệ thống giáo trình lý thuyết cấu trúc dữ liệu & giải thuật tinh gọn, giải thích bản chất kèm độ phức tạp không thời gian.
              </p>
            </div>
          </div>

          {/* Search & Categories Bar */}
          <div className="mt-4 d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
            {/* Search Input */}
            <div className="position-relative flex-grow-1" style={{ maxWidth: 448 }}>
              <Search size={16} className="position-absolute top-50 start-0 translate-middle-y ms-3 text-body-secondary" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Tìm bài học lý thuyết (vd: Dynamic Array, Stack, Binary Search...)"
                className="form-control fs-xs bg-surface border"
                style={{ paddingLeft: '2.5rem' }}
              />
            </div>

            {/* Category Filter Pills */}
            <div className="d-flex align-items-center gap-2 overflow-x-auto pb-1 pb-sm-0">
              <button type="button" onClick={() => setSelectedCategory('ALL')} className={pillCls(selectedCategory === 'ALL')}>
                Tất cả ({theories.length})
              </button>
              {categories.map((cat) => (
                <button key={cat} type="button" onClick={() => setSelectedCategory(cat)} className={pillCls(selectedCategory === cat)}>
                  {cat}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Theories Content Grid */}
        {isLoading ? (
          <div className="d-flex flex-column align-items-center justify-content-center gap-3 py-5 my-4 text-body-secondary">
            <Spinner animation="border" size="sm" />
            <span className="fs-xs">Đang tải danh sách bài học lý thuyết từ cơ sở dữ liệu...</span>
          </div>
        ) : filteredTheories.length > 0 ? (
          <div className="row g-3">
            {filteredTheories.map((theory) => (
              <div key={theory.slug || theory.id} className="col-12 col-md-6 col-lg-4">
                <Link
                  to={`/theories/${theory.slug}`}
                  className="card h-100 justify-content-between p-4 text-body text-decoration-none hover-bg"
                >
                  <div>
                    {/* Category Pill */}
                    <div className="d-flex align-items-center justify-content-between mb-3">
                      <span className="badge rounded-1 bg-body-tertiary border fs-2xs text-body">
                        {theory.category}
                      </span>
                      <span className="fs-2xs font-monospace text-body-secondary">
                        Bài #{theory.orderIndex || 1}
                      </span>
                    </div>

                    {/* Title */}
                    <h3 className="fs-base fw-semibold text-body-emphasis mb-2">
                      {theory.title}
                    </h3>

                    {/* Summary */}
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
        ) : (
          <div className="card p-5 text-center text-body-secondary">
            <p className="mb-0">Không tìm thấy bài học lý thuyết phù hợp với từ khóa.</p>
          </div>
        )}
      </main>
    </div>
  );
}
