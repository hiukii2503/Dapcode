import React from 'react';
import { Link } from 'react-router-dom';
import { Offcanvas } from 'react-bootstrap';
import { Check, ExternalLink, Trophy } from 'lucide-react';
import DifficultyBadge from './DifficultyBadge';

export default function RoadmapTopicDrawer({
  node,
  isOpen,
  onClose,
  problems = [],
  completedSlugs = new Set(),
  onToggleComplete,
}) {
  if (!node) return null;

  const topicProblems = problems.filter(
    (p) => p.category?.toLowerCase() === node.name?.toLowerCase() ||
           p.category?.toLowerCase() === node.categoryKey?.toLowerCase()
  );

  const completedCount = topicProblems.filter((p) => completedSlugs.has(p.slug)).length;
  const total = topicProblems.length || node.total || 0;
  const percent = total > 0 ? Math.round((completedCount / total) * 100) : 0;

  return (
    <Offcanvas show={isOpen} onHide={onClose} placement="end" className="offcanvas-md-width">
      {/* Drawer Header */}
      <Offcanvas.Header closeButton className="border-bottom bg-body-tertiary px-4 py-3">
        <div>
          <Offcanvas.Title as="h2" className="fs-sm fw-semibold text-body-emphasis mb-0">
            {node.name}
          </Offcanvas.Title>
          <div className="mt-1 fs-2xs text-body-secondary">
            Đã giải <strong className="text-success font-monospace">{completedCount}</strong> / {total} bài ({percent}%)
          </div>
        </div>
      </Offcanvas.Header>

      {/* Progress bar */}
      <div className="progress rounded-0 flex-shrink-0 border-bottom" style={{ height: 4 }}>
        <div className="progress-bar bg-contrast" style={{ width: `${percent}%` }} />
      </div>

      {/* Drawer Body Problem List */}
      <Offcanvas.Body className="d-flex flex-column gap-2 p-3">
        {topicProblems.length > 0 ? (
          topicProblems.map((problem) => {
            const isCompleted = completedSlugs.has(problem.slug);

            return (
              <div
                key={problem.slug || problem.id}
                className={`d-flex align-items-center justify-content-between rounded-3 border px-3 py-2 ${
                  isCompleted ? 'bg-success-subtle border-success-subtle' : 'bg-body-tertiary'
                }`}
              >
                <div className="d-flex align-items-center gap-3 min-w-0">
                  {/* Checkbox */}
                  <button
                    type="button"
                    onClick={() => onToggleComplete(problem.slug)}
                    className={`d-flex flex-shrink-0 align-items-center justify-content-center rounded-1 border p-0 cursor-pointer ${
                      isCompleted ? 'bg-success border-success text-white' : 'bg-surface'
                    }`}
                    style={{ width: 18, height: 18 }}
                  >
                    <Check size={12} className={isCompleted ? '' : 'invisible'} />
                  </button>

                  {/* Title */}
                  <Link
                    to={`/problems/${problem.slug}`}
                    className="text-truncate fs-xs fw-medium link-body-emphasis text-decoration-none"
                  >
                    {problem.title}
                  </Link>
                </div>

                <div className="d-flex align-items-center gap-2 flex-shrink-0 ps-2">
                  <DifficultyBadge difficulty={problem.difficulty} size="xs" />
                  <Link to={`/problems/${problem.slug}`} className="d-inline-flex p-1 text-body-secondary" title="Mở workspace">
                    <ExternalLink size={14} />
                  </Link>
                </div>
              </div>
            );
          })
        ) : (
          <div className="p-5 text-center fs-xs text-body-secondary">
            Đang đồng bộ dữ liệu cho nhóm chủ đề này...
          </div>
        )}
      </Offcanvas.Body>

      {/* Footer Summary */}
      <div className="border-top bg-body-tertiary p-3 d-flex align-items-center justify-content-between fs-xs text-body-secondary">
        <div className="d-flex align-items-center gap-2">
          <Trophy size={14} className="text-primary" />
          <span>NeetCode 150 Standard</span>
        </div>
        <span className="font-monospace">{topicProblems.length} bài tập</span>
      </div>
    </Offcanvas>
  );
}
