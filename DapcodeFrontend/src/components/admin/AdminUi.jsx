import React, { createContext, useContext, useState, useCallback, useEffect, useId } from 'react';
import { Modal, Form, Toast, ToastContainer, Spinner as BsSpinner } from 'react-bootstrap';
import { CheckCircle2, AlertCircle, ChevronLeft, ChevronRight } from 'lucide-react';

export const inputCls = 'form-control';

export const selectCls = 'form-select';

export const btnPrimary = 'btn btn-primary d-inline-flex align-items-center justify-content-center gap-2';

export const btnGhost = 'btn btn-ghost d-inline-flex align-items-center justify-content-center gap-2';

export const btnDanger = 'btn btn-danger d-inline-flex align-items-center justify-content-center gap-2';

export const iconBtn = 'icon-btn';

export function Card({ className = '', children }) {
  return <div className={`card border-0 ${className}`}>{children}</div>;
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="mb-4 d-flex flex-wrap align-items-end justify-content-between gap-3">
      <div>
        <h1 className="h1 mb-0">{title}</h1>
        {subtitle && <p className="mt-1 mb-0 text-body-secondary">{subtitle}</p>}
      </div>
      {actions && <div className="d-flex align-items-center gap-2">{actions}</div>}
    </div>
  );
}

export function Field({ label, hint, children }) {
  return (
    <label className="d-block">
      <span className="form-label d-block">{label}</span>
      {children}
      {hint && <span className="form-text d-block">{hint}</span>}
    </label>
  );
}

export function Spinner({ label }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center gap-2 py-5 text-body-secondary">
      <BsSpinner animation="border" size="sm" />
      {label && <span>{label}</span>}
    </div>
  );
}

export function Empty({ children }) {
  return <div className="py-5 text-center text-body-secondary">{children}</div>;
}

export function Badge({ tone = 'neutral', children }) {
  const tones = {
    neutral: 'bg-body-secondary text-body-secondary',
    green: 'bg-success bg-opacity-10 text-success',
    red: 'bg-danger bg-opacity-10 text-danger',
    orange: 'bg-primary bg-opacity-25 text-primary',
    blue: 'bg-info bg-opacity-10 text-info',
  };
  return <span className={`badge rounded-pill ${tones[tone]}`}>{children}</span>;
}

export function Toggle({ checked, onChange, label }) {
  const id = useId();
  return (
    <Form.Check
      type="switch"
      id={id}
      className="form-switch-success mb-0"
      checked={checked}
      onChange={(e) => onChange(e.target.checked)}
      label={label}
    />
  );
}

export function Dialog({ open, onClose, title, children, footer, size = 'md' }) {
  const sizes = { sm: 'sm', md: undefined, lg: 'lg', xl: 'xl' };

  return (
    <Modal show={open} onHide={onClose} size={sizes[size]} centered scrollable>
      <Modal.Header closeButton>
        <Modal.Title as="h2">{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>{children}</Modal.Body>
      {footer && <Modal.Footer>{footer}</Modal.Footer>}
    </Modal>
  );
}

export function ConfirmDialog({ open, onClose, onConfirm, title, message, confirmLabel = 'Xác nhận', busy, danger = true }) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={title}
      size="sm"
      footer={
        <>
          <button type="button" onClick={onClose} className={btnGhost}>
            Hủy
          </button>
          <button type="button" onClick={onConfirm} disabled={busy} className={danger ? btnDanger : btnPrimary}>
            {busy && <BsSpinner animation="border" size="sm" />}
            {confirmLabel}
          </button>
        </>
      }
    >
      <p className="mb-0 text-body-secondary">{message}</p>
    </Dialog>
  );
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="d-flex align-items-center justify-content-center gap-1 border-top p-3">
      <button type="button" className={iconBtn} disabled={page <= 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft size={16} />
      </button>
      <span className="px-3 text-body-secondary">
        {page + 1} / {totalPages}
      </span>
      <button type="button" className={iconBtn} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        <ChevronRight size={16} />
      </button>
    </div>
  );
}

const ToastContext = createContext({ push: () => {} });

export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);

  const push = useCallback((type, message) => {
    const id = Date.now() + Math.random();
    setItems((prev) => [...prev, { id, type, message }]);
    setTimeout(() => setItems((prev) => prev.filter((t) => t.id !== id)), 3200);
  }, []);

  return (
    <ToastContext.Provider value={{ push }}>
      {children}
      <ToastContainer position="bottom-end" containerPosition="fixed" className="p-3">
        {items.map((t) => (
          <Toast key={t.id} className="app-toast w-auto shadow">
            <Toast.Body className="d-flex align-items-center gap-2 px-3 py-2">
              {t.type === 'error' ? (
                <AlertCircle size={16} className="text-danger" />
              ) : (
                <CheckCircle2 size={16} className="text-success" />
              )}
              <span>{t.message}</span>
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
    </ToastContext.Provider>
  );
}

export const useToast = () => useContext(ToastContext);

export function useDebounced(value, delay = 300) {
  const [v, setV] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setV(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return v;
}
