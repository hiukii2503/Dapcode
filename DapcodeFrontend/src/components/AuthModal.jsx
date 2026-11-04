import React, { useState, useEffect } from 'react';
import { Modal, Spinner } from 'react-bootstrap';
import { Sparkles, User, ShieldCheck, AlertCircle, Lock, Eye, EyeOff, Shield, ArrowLeft } from 'lucide-react';
import api from '../api/client';

export default function AuthModal({ isOpen, onClose, onLoginSuccess, promptMessage }) {
  const [mode, setMode] = useState('default'); // 'default' | 'admin'
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState(null);

  // Admin form state
  const [adminUsername, setAdminUsername] = useState('admin');
  const [adminPassword, setAdminPassword] = useState('admin123');
  const [showPassword, setShowPassword] = useState(false);

  // Reset state when modal is opened
  useEffect(() => {
    if (isOpen) {
      setMode('default');
      setErrorMsg(null);
      setIsLoading(false);
      setAdminUsername('admin');
      setAdminPassword('admin123');
      setShowPassword(false);
    }
  }, [isOpen]);

  const handleClose = () => {
    setMode('default');
    setErrorMsg(null);
    onClose();
  };

  const handleGoogleLogin = () => {
    setIsLoading(true);
    setErrorMsg(null);
    sessionStorage.setItem('dapcode_auth_return', window.location.pathname);
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  const handleQuickDemoLogin = async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const res = await api.post('/auth/demo');
      const data = res?.data || res;
      if (data && data.token) {
        localStorage.setItem('dapcode_token', data.token);
        const userData = data.user || {
          name: 'LiệtCode Master',
          email: 'demo@lietcode.io',
          avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80',
          streak: 3,
          xp: 120,
        };
        localStorage.setItem('dapcode_user', JSON.stringify(userData));
        if (onLoginSuccess) onLoginSuccess(userData);
        handleClose();
        return;
      }
    } catch {
      // Fallback local test session
      const demoUser = {
        name: 'LiệtCode Master',
        email: 'demo@lietcode.io',
        avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80',
        streak: 3,
        xp: 120,
      };
      localStorage.setItem('dapcode_user', JSON.stringify(demoUser));
      localStorage.removeItem('dapcode_token');
      if (onLoginSuccess) onLoginSuccess(demoUser);
      handleClose();
    } finally {
      setIsLoading(false);
    }
  };

  const handleAdminLoginSubmit = async (e) => {
    if (e) e.preventDefault();
    if (!adminUsername.trim() || !adminPassword.trim()) {
      setErrorMsg('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!');
      return;
    }

    setIsLoading(true);
    setErrorMsg(null);

    try {
      const res = await api.post('/auth/admin-login', {
        usernameOrEmail: adminUsername.trim(),
        password: adminPassword.trim(),
      });
      const data = res?.data || res;

      if (data && data.token) {
        localStorage.setItem('dapcode_token', data.token);
        const adminUser = data.user || {
          id: '0529d450-19bc-4762-931b-28c7daf3d6ec',
          username: adminUsername.trim(),
          name: 'LiệtCode Admin',
          email: 'admin@dapcode.io',
          role: 'ROLE_ADMIN',
          avatarUrl: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin',
          streak: 30,
          xp: 9999,
        };
        localStorage.setItem('dapcode_user', JSON.stringify(adminUser));
        if (onLoginSuccess) onLoginSuccess(adminUser);
        setIsLoading(false);
        handleClose();
        return;
      }
      throw new Error('Không nhận được token xác thực quản trị viên.');
    } catch (err) {
      console.error('Lỗi đăng nhập admin:', err);
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Tài khoản hoặc mật khẩu không chính xác!';
      setErrorMsg(msg);
      setIsLoading(false);
    }
  };

  return (
    <Modal show={isOpen} onHide={handleClose} centered style={{ zIndex: 1060 }}>
      <Modal.Body className="position-relative p-4">
        <button
          type="button"
          className="btn-close position-absolute top-0 end-0 m-3"
          aria-label="Đóng"
          onClick={handleClose}
        />

        {mode === 'default' ? (
          <>
            {/* Modal Brand Header */}
            <div className="text-center mb-4">
              <div
                className="mx-auto mb-3 d-flex align-items-center justify-content-center rounded-3 bg-contrast"
                style={{ width: 44, height: 44 }}
              >
                <Sparkles size={22} />
              </div>
              <h2 className="fs-base fw-semibold text-body-emphasis mb-0">Đăng nhập vào LiệtCode</h2>
              <p className="mt-1 mb-0 fs-xs text-body-secondary">
                Lưu chuỗi streak, lịch sử nộp bài và nhận phản hồi AI Tutor không giới hạn.
              </p>
            </div>

            {/* Action Prompt Banner if opened from submit/run/ai */}
            {promptMessage && (
              <div className="mb-3 d-flex align-items-start gap-2 rounded-3 border border-primary-subtle bg-primary-subtle text-primary-emphasis p-3 fs-xs">
                <AlertCircle size={16} className="flex-shrink-0 mt-1" />
                <span>{promptMessage}</span>
              </div>
            )}

            {/* Error Banner */}
            {errorMsg && (
              <div className="alert alert-danger mb-3 p-3 rounded-3 fs-xs d-flex align-items-start gap-2">
                <AlertCircle size={16} className="flex-shrink-0 mt-1" />
                <span>{errorMsg}</span>
              </div>
            )}

            {/* Action Buttons */}
            <div className="d-flex flex-column gap-3">
              {/* Google Login Button */}
              <button
                type="button"
                disabled={isLoading}
                onClick={handleGoogleLogin}
                className="btn btn-ghost border w-100 d-flex align-items-center justify-content-center gap-3 py-2 fs-xs"
              >
                {isLoading ? (
                  <Spinner animation="border" size="sm" className="text-body-secondary" />
                ) : (
                  <svg width={16} height={16} viewBox="0 0 24 24">
                    <path
                      fill="#4285F4"
                      d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                    />
                    <path
                      fill="#34A853"
                      d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                    />
                    <path
                      fill="#FBBC05"
                      d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
                    />
                    <path
                      fill="#EA4335"
                      d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
                    />
                  </svg>
                )}
                <span>Tiếp tục với Google</span>
              </button>

              <div className="d-flex align-items-center gap-2 text-body-secondary fs-2xs fw-medium text-uppercase">
                <hr className="flex-grow-1 my-0" />
                <span>hoặc</span>
                <hr className="flex-grow-1 my-0" />
              </div>

              {/* Quick Demo Login */}
              <button
                type="button"
                disabled={isLoading}
                onClick={handleQuickDemoLogin}
                className="btn btn-contrast w-100 d-flex align-items-center justify-content-center gap-2 py-2 fs-xs"
              >
                {isLoading ? <Spinner animation="border" size="sm" /> : <User size={16} />}
                <span>Đăng nhập nhanh (Guest / Demo)</span>
              </button>

              {/* Admin Login Trigger Button */}
              <button
                type="button"
                disabled={isLoading}
                onClick={() => {
                  setMode('admin');
                  setErrorMsg(null);
                }}
                className="btn btn-soft-primary border border-primary border-opacity-25 w-100 d-flex align-items-center justify-content-center gap-2 py-2 fs-xs fw-semibold"
              >
                <ShieldCheck size={16} />
                <span>Đăng nhập quyền Admin</span>
              </button>
            </div>

            <div className="mt-4 d-flex align-items-center justify-content-center gap-2 fs-2xs text-body-secondary">
              <ShieldCheck size={14} className="text-success" />
              <span>Bảo mật an toàn qua JWT & Spring Security</span>
            </div>
          </>
        ) : (
          <>
            {/* Admin Login Form View */}
            <div className="text-center mb-3">
              <div
                className="mx-auto mb-3 d-flex align-items-center justify-content-center rounded-3 bg-primary bg-opacity-10 text-primary border border-primary border-opacity-25"
                style={{ width: 44, height: 44 }}
              >
                <Shield size={22} />
              </div>
              <h2 className="fs-base fw-semibold text-body-emphasis mb-0">Đăng nhập Quản trị viên</h2>
              <p className="mt-1 mb-0 fs-xs text-body-secondary">
                Nhập tài khoản quản trị để truy cập trang quản lý hệ thống.
              </p>
            </div>

            {/* Error Banner */}
            {errorMsg && (
              <div className="alert alert-danger mb-3 p-3 rounded-3 fs-xs d-flex align-items-start gap-2">
                <AlertCircle size={16} className="flex-shrink-0 mt-1" />
                <div>
                  <div className="fw-semibold">Đăng nhập thất bại:</div>
                  <div>{errorMsg}</div>
                </div>
              </div>
            )}

            <form onSubmit={handleAdminLoginSubmit} className="d-flex flex-column gap-3">
              <div>
                <label className="form-label fs-xs fw-medium text-body-secondary mb-1">
                  Tên đăng nhập / Email
                </label>
                <div className="position-relative">
                  <div
                    className="pe-none position-absolute top-50 translate-middle-y d-flex text-body-secondary"
                    style={{ left: 12 }}
                  >
                    <User size={15} />
                  </div>
                  <input
                    type="text"
                    required
                    value={adminUsername}
                    onChange={(e) => {
                      setAdminUsername(e.target.value);
                      setErrorMsg(null);
                    }}
                    placeholder="admin hoặc admin@dapcode.io"
                    className="form-control ps-5 py-2 fs-xs"
                    autoFocus
                  />
                </div>
              </div>

              <div>
                <div className="d-flex align-items-center justify-content-between mb-1">
                  <label className="form-label fs-xs fw-medium text-body-secondary mb-0">
                    Mật khẩu
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setAdminUsername('admin');
                      setAdminPassword('admin123');
                      setErrorMsg(null);
                    }}
                    className="btn btn-link p-0 border-0 fs-2xs text-primary d-flex align-items-center gap-1 text-decoration-none"
                  >
                    <Sparkles size={11} />
                    <span>Điền mẫu</span>
                  </button>
                </div>
                <div className="position-relative">
                  <div
                    className="pe-none position-absolute top-50 translate-middle-y d-flex text-body-secondary"
                    style={{ left: 12 }}
                  >
                    <Lock size={15} />
                  </div>
                  <input
                    type={showPassword ? 'text' : 'password'}
                    required
                    value={adminPassword}
                    onChange={(e) => {
                      setAdminPassword(e.target.value);
                      setErrorMsg(null);
                    }}
                    placeholder="••••••••"
                    className="form-control ps-5 pe-5 py-2 fs-xs"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="btn btn-link position-absolute end-0 top-50 translate-middle-y p-2 text-body-secondary text-decoration-none border-0"
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="btn btn-primary w-100 d-flex align-items-center justify-content-center gap-2 py-2 fs-xs fw-semibold mt-1"
              >
                {isLoading ? <Spinner animation="border" size="sm" /> : <ShieldCheck size={16} />}
                <span>Đăng nhập Admin</span>
              </button>

              <button
                type="button"
                disabled={isLoading}
                onClick={() => {
                  setMode('default');
                  setErrorMsg(null);
                }}
                className="btn btn-ghost border-0 w-100 py-1 fs-xs text-body-secondary d-flex align-items-center justify-content-center gap-1"
              >
                <ArrowLeft size={14} />
                <span>Quay lại lựa chọn đăng nhập</span>
              </button>
            </form>
          </>
        )}
      </Modal.Body>
    </Modal>
  );
}
