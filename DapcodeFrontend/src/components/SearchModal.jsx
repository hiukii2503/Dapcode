import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Modal, Spinner } from 'react-bootstrap';
import { Search, CornerDownLeft } from 'lucide-react';
import DifficultyBadge from './DifficultyBadge';
import { getProblems } from '../api/problems';

export default function SearchModal({ isOpen, onClose, problems: initialProblems }) {
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [problems, setProblems] = useState(initialProblems || []);
  const [isLoading, setIsLoading] = useState(false);
  const inputRef = useRef(null);
  const navigate = useNavigate();

  // If no problems were provided via prop, fetch real problems from backend
  useEffect(() => {
    if (isOpen) {
      if (!initialProblems || initialProblems.length === 0) {
        setIsLoading(true);
        getProblems({ size: 250 })
          .then((res) => {
            const list = res?.data?.content || res?.content || res?.data || [];
            if (Array.isArray(list)) {
              setProblems(list);
            }
          })
          .catch((err) => {
            console.error('Lỗi khi tải danh sách bài tập:', err);
          })
          .finally(() => {
            setIsLoading(false);
          });
      } else {
        setProblems(initialProblems);
      }
    }
  }, [isOpen, initialProblems]);

  // Filter problems based on query
  const filteredProblems = query.trim() === ''
    ? problems.slice(0, 10)
    : problems.filter(
        (p) =>
          p.title?.toLowerCase().includes(query.toLowerCase()) ||
          p.category?.toLowerCase().includes(query.toLowerCase()) ||
          p.slug?.toLowerCase().includes(query.toLowerCase())
      ).slice(0, 12);

  // Reset when opened (Escape / backdrop close is handled by Modal)
  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setSelectedIndex(0);
    }
  }, [isOpen]);

  // Keyboard navigation inside modal
  const handleInputKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev + 1) % Math.max(1, filteredProblems.length));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => (prev - 1 + filteredProblems.length) % Math.max(1, filteredProblems.length));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const target = filteredProblems[selectedIndex];
      if (target) {
        onClose();
        navigate(`/problems/${target.slug}`);
      }
    }
  };

  return (
    <Modal
      show={isOpen}
      onHide={onClose}
      onEntered={() => inputRef.current?.focus()}
      dialogClassName="mt-5 pt-sm-5"
      contentClassName="overflow-hidden"
    >
      {/* Search Input Bar */}
      <div className="d-flex align-items-center gap-3 px-3 py-3 border-bottom bg-body-tertiary">
        <Search size={16} className="text-body-secondary flex-shrink-0" />
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setSelectedIndex(0);
          }}
          onKeyDown={handleInputKeyDown}
          placeholder="Tìm kiếm bài tập theo tên, danh mục hoặc slug..."
          className="form-control flex-grow-1 border-0 bg-transparent shadow-none p-0"
        />
        {isLoading && <Spinner animation="border" size="sm" className="text-body-secondary" />}
        <button type="button" className="btn-close" aria-label="Đóng" onClick={onClose} />
      </div>

      {/* Results List */}
      <div className="overflow-y-auto p-2" style={{ maxHeight: 320 }}>
        {filteredProblems.length > 0 ? (
          filteredProblems.map((problem, idx) => {
            const isSelected = selectedIndex === idx;
            return (
              <div
                key={problem.slug || problem.id}
                onClick={() => {
                  onClose();
                  navigate(`/problems/${problem.slug}`);
                }}
                onMouseEnter={() => setSelectedIndex(idx)}
                className={`d-flex align-items-center justify-content-between gap-2 px-3 py-2 rounded-3 cursor-pointer ${
                  isSelected ? 'bg-body-tertiary text-body-emphasis' : ''
                }`}
              >
                <div className="d-flex align-items-center gap-3 min-w-0">
                  <span className="fs-xs fw-medium text-body-emphasis text-truncate">{problem.title}</span>
                  <span className="fs-2xs text-body-secondary d-none d-sm-inline">{problem.category}</span>
                </div>

                <div className="d-flex align-items-center gap-2 flex-shrink-0">
                  <DifficultyBadge difficulty={problem.difficulty} size="xs" />
                  {isSelected && <CornerDownLeft size={14} className="text-body-secondary" />}
                </div>
              </div>
            );
          })
        ) : (
          <div className="py-5 text-center fs-xs text-body-secondary">
            {isLoading ? 'Đang tải dữ liệu bài tập từ database...' : `Không tìm thấy bài tập nào với từ khóa "${query}".`}
          </div>
        )}
      </div>
    </Modal>
  );
}
