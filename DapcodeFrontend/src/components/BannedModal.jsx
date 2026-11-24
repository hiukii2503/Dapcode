import React, { useState, useEffect } from 'react';
import { Modal } from 'react-bootstrap';
import { ShieldAlert, AlertTriangle, Mail } from 'lucide-react';

export default function BannedModal() {
  const [banInfo, setBanInfo] = useState(null);

  useEffect(() => {
    const checkStoredBan = () => {
      try {
        const raw = sessionStorage.getItem('dapcode_ban_info');
        if (raw) {
          const parsed = JSON.parse(raw);
          sessionStorage.removeItem('dapcode_ban_info');
          setBanInfo({
            reason: parsed?.reason || 'Vi phạm quy định và điều khoản sử dụng của hệ thống.',
            email: parsed?.email || null,
          });
        }
      } catch {
        sessionStorage.removeItem('dapcode_ban_info');
      }
    };

    checkStoredBan();

    const handleBannedEvent = (e) => {
      const detail = e?.detail || {};
      setBanInfo({
        reason: detail.reason || 'Vi phạm quy định và điều khoản sử dụng của hệ thống.',
        email: detail.email || null,
      });
    };

    window.addEventListener('dapcode-user-banned', handleBannedEvent);
    return () => window.removeEventListener('dapcode-user-banned', handleBannedEvent);
  }, []);

  const handleClose = () => {
    setBanInfo(null);
  };

  return (
    <Modal show={Boolean(banInfo)} onHide={handleClose} centered style={{ zIndex: 1080 }}>
      <Modal.Body className="position-relative p-4">
        <button
          type="button"
          className="btn-close position-absolute top-0 end-0 m-3"
          aria-label="Đóng"
          onClick={handleClose}
        />

        {banInfo && (
          <>
            <div className="d-flex flex-column align-items-center text-center">
              <div
                className="mb-3 d-flex align-items-center justify-content-center rounded-4 bg-danger-subtle border border-danger-subtle text-danger"
                style={{ width: 56, height: 56 }}
              >
                <ShieldAlert size={28} />
              </div>

              <h2 className="fs-lg fw-bold text-body-emphasis mb-0">
                Tài khoản của bạn đã bị khóa
              </h2>

              {banInfo.email && (
                <p className="mt-1 mb-0 fs-xs font-monospace text-body-secondary">
                  {banInfo.email}
                </p>
              )}

              <p className="mt-2 mb-0 fs-xs text-body-secondary">
                Hệ thống đã từ chối yêu cầu đăng nhập vì tài khoản này hiện đang bị vô hiệu hóa bởi Quản trị viên.
              </p>
            </div>

            <div className="mt-3 rounded-3 border border-danger-subtle bg-danger-subtle p-3">
              <div className="d-flex align-items-center gap-2 fs-xs fw-semibold text-danger mb-1">
                <AlertTriangle size={14} className="flex-shrink-0" />
                <span>Nguyên nhân khóa tài khoản:</span>
              </div>
              <p className="mb-0 fs-sm fw-medium text-body-emphasis text-break">
                {banInfo.reason}
              </p>
            </div>

            <div className="mt-3 d-flex align-items-center gap-2 rounded-3 bg-body-tertiary px-3 py-2 fs-xs text-body-secondary">
              <Mail size={16} className="flex-shrink-0" />
              <span>Nếu bạn cho rằng đây là sự nhầm lẫn, vui lòng liên hệ Quản trị viên để được hỗ trợ mở khóa.</span>
            </div>

            <button
              type="button"
              onClick={handleClose}
              className="btn btn-danger mt-4 w-100 rounded-3 py-2 fs-xs fw-semibold text-white"
            >
              Đã hiểu
            </button>
          </>
        )}
      </Modal.Body>
    </Modal>
  );
}
