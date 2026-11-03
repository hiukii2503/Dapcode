import React from 'react';
import { Modal } from 'react-bootstrap';
import { Flame, Calendar, Award } from 'lucide-react';

export default function StreakModal({ isOpen, onClose, streakCount = 1 }) {
  const daysOfWeek = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
  const todayIdx = (new Date().getDay() + 6) % 7; // Monday = 0

  return (
    <Modal show={isOpen} onHide={onClose} size="sm" centered>
      <Modal.Body className="position-relative p-4">
        <button type="button" className="btn-close position-absolute top-0 end-0 m-3" aria-label="Đóng" onClick={onClose} />

        {/* Header Flame */}
        <div className="text-center mb-4">
          <div
            className="mx-auto mb-2 d-flex align-items-center justify-content-center rounded-4 border bg-body-tertiary"
            style={{ width: 48, height: 48 }}
          >
            <Flame size={24} className="text-primary" />
          </div>
          <div className="fs-2xl fw-bold font-monospace text-body-emphasis">
            {streakCount} <span className="fs-sm fw-medium text-primary" style={{ fontFamily: 'var(--bs-body-font-family)' }}>ngày liên tục</span>
          </div>
          <p className="mt-1 mb-0 fs-xs text-body-secondary">
            Giải ít nhất 1 bài mỗi ngày để tích lũy kiến thức và duy trì phong độ phỏng vấn.
          </p>
        </div>

        {/* Weekly Calendar */}
        <div className="rounded-3 border bg-body-tertiary p-3 mb-3">
          <div className="d-flex align-items-center gap-2 fs-xs text-body-secondary mb-2">
            <Calendar size={14} />
            <span>Tuần này</span>
          </div>
          <div className="d-flex justify-content-between gap-1 text-center">
            {daysOfWeek.map((day, idx) => {
              const isToday = idx === todayIdx;
              const isPast = idx < todayIdx;
              return (
                <div key={day} className="d-flex flex-column align-items-center gap-1">
                  <span className="fs-2xs text-body-secondary">{day}</span>
                  <div
                    className={`d-flex align-items-center justify-content-center rounded-2 fs-xs fw-medium font-monospace ${
                      isToday ? 'bg-contrast' : isPast ? 'bg-surface border' : 'border text-body-tertiary'
                    }`}
                    style={{ width: 28, height: 28, borderStyle: !isToday && !isPast ? 'dashed' : undefined }}
                  >
                    {isToday ? '🔥' : isPast ? '✓' : '•'}
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Milestone badge */}
        <div className="d-flex align-items-center gap-3 rounded-3 border bg-body-tertiary p-3 fs-xs">
          <Award size={20} className="flex-shrink-0" />
          <div>
            <div className="fw-semibold text-body-emphasis">Mục tiêu tiếp theo: 7 ngày</div>
            <div className="fs-2xs text-body-secondary">Mở khóa huy hiệu &quot;Algorithm Apprentice&quot;</div>
          </div>
        </div>
      </Modal.Body>
    </Modal>
  );
}
