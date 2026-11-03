import React from 'react';
import { Flame } from 'lucide-react';

export default function ProgressSidebar({ problems = [], completedSlugs = new Set() }) {
  const total = problems.length;
  const solved = problems.filter((p) => completedSlugs.has(p.slug)).length;

  const stat = (level) => {
    const list = problems.filter((p) => p.difficulty?.toLowerCase() === level);
    return { total: list.length, done: list.filter((p) => completedSlugs.has(p.slug)).length };
  };

  const rows = [
    { label: 'Easy', color: '#00B8A3', ...stat('easy') },
    { label: 'Med.', color: '#FFB800', ...stat('medium') },
    { label: 'Hard', color: '#FF375F', ...stat('hard') },
  ];

  const radius = 42;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference - (total ? solved / total : 0) * circumference;

  let streak = 0;
  try {
    const u = JSON.parse(localStorage.getItem('dapcode_user') || 'null');
    streak = u?.streakCount ?? u?.streak ?? 0;
  } catch {
    streak = 0;
  }

  return (
    <div className="d-flex flex-column gap-3">
      <div className="card border-0 p-3">
        <div className="d-flex align-items-center gap-4">
          <div className="position-relative d-flex flex-shrink-0 align-items-center justify-content-center">
            <svg width={112} height={112} viewBox="0 0 100 100" style={{ transform: 'rotate(-90deg)' }}>
              <circle cx="50" cy="50" r={radius} fill="none" strokeWidth="5" style={{ stroke: 'var(--bs-border-color)' }} />
              <circle
                cx="50"
                cy="50"
                r={radius}
                fill="none"
                strokeWidth="5"
                strokeLinecap="round"
                stroke="#FFA116"
                strokeDasharray={circumference}
                strokeDashoffset={offset}
                style={{ transition: 'stroke-dashoffset .7s ease-out' }}
              />
            </svg>
            <div className="position-absolute d-flex flex-column align-items-center lh-sm">
              <span className="fs-2xl fw-semibold text-body-emphasis">{solved}</span>
              <span className="fs-xs text-body-secondary">/ {total}</span>
              <span className="mt-1 fs-2xs text-body-secondary">Solved</span>
            </div>
          </div>

          <div className="flex-grow-1 d-flex flex-column gap-2">
            {rows.map((r) => (
              <div key={r.label} className="rounded-1 bg-body-tertiary px-3 py-2">
                <div className="fs-xs fw-medium" style={{ color: r.color }}>
                  {r.label}
                </div>
                <div className="fs-xs text-body-secondary">
                  <span className="fw-medium text-body-emphasis">{r.done}</span>/{r.total}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="card border-0 flex-row align-items-center justify-content-between px-3 py-3">
        <div className="d-flex align-items-center gap-2 text-body-secondary">
          <Flame size={20} className="text-primary" />
          <span>Daily Streak</span>
        </div>
        <span className="fw-semibold text-body-emphasis">
          {streak} <span className="fs-xs fw-normal text-body-secondary">days</span>
        </span>
      </div>
    </div>
  );
}
