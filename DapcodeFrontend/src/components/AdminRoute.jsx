import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import {
  Shield,
  ShieldAlert,
  ShieldCheck,
  ArrowLeft,
  User,
  Lock,
  Eye,
  EyeOff,
  LogOut,
  Sparkles,
  KeyRound,
} from 'lucide-react';
import { getCurrentUser, loginAdmin } from '../api/auth';
import DapcodeLogo from './DapcodeLogo';

export default function AdminRoute({ children }) {
  const [state, setState] = useState('checking'); // 'checking' | 'ok' | 'denied'
  const [currentUser, setCurrentUser] = useState(null);
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const checkAdminAuth = () => {
    const token = localStorage.getItem('dapcode_token');

    // 1. Không có token -> Chưa đăng nhập, từ chối và hiện form
    if (!token) {
      localStorage.removeItem('dapcode_user');
      setCurrentUser(null);
      setState('denied');
      return;
    }

    // 2. Có token -> Gửi lên Server để verify tính hợp lệ chữ ký mật mã qua /api/v1/users/me
    getCurrentUser()
      .then((res) => {
        const u = res?.data || res;
        // Kiểm tra chặt chẽ vai trò ROLE_ADMIN từ phía server database
        if (u && u.role === 'ROLE_ADMIN') {
          localStorage.setItem('dapcode_user', JSON.stringify(u));
          setCurrentUser(u);
          setState('ok');
        } else {
          // Token hợp lệ nhưng tài khoản không có quyền Admin
          setCurrentUser(u);
          setState('denied');
        }
      })
      .catch(() => {
        // Token giả mạo, hết hạn hoặc không khớp chữ ký số bí mật (HTTP 401/403)
        // Thu hồi token và dữ liệu trong localStorage
        localStorage.removeItem('dapcode_token');
        localStorage.removeItem('dapcode_user');
        setCurrentUser(null);
        setState('denied');
      });
  };

  useEffect(() => {
    checkAdminAuth();
  }, []);

  const handlePasswordLogin = async (e) => {
    if (e) e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setErrorMessage('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!');
      return;
    }

    setIsLoading(true);
    setErrorMessage('');

    try {
      const res = await loginAdmin(username.trim(), password.trim());
      const data = res?.data || res;

      if (data && data.token) {
        localStorage.setItem('dapcode_token', data.token);
        const userObj = data.user || {
          username: username.trim(),
          email: 'admin@dapcode.io',
          role: 'ROLE_ADMIN',
        };
        localStorage.setItem('dapcode_user', JSON.stringify(userObj));
        setCurrentUser(userObj);
        setIsLoading(false);
        setState('ok');
        return;
      }
      throw new Error('Không nhận được token xác thực quản trị viên.');
    } catch (err) {
      console.error('Lỗi đăng nhập admin:', err);
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Tài khoản hoặc mật khẩu không chính xác. Tài khoản mặc định: admin / admin123';
      setErrorMessage(msg);
      setIsLoading(false);
    }
  };

  const handleFillDemo = () => {
    setUsername('admin');
    setPassword('admin123');
    setErrorMessage('');
  };

  const handleGoogleLogin = () => {
    setIsLoading(true);
    setErrorMessage('');
    sessionStorage.setItem('dapcode_auth_return', window.location.pathname);
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  const handleLogoutOtherAccount = () => {
    localStorage.removeItem('dapcode_user');
    localStorage.removeItem('dapcode_token');
    setCurrentUser(null);
    setErrorMessage('');
    setState('denied');
  };

  if (state === 'checking') {
    return (
      <div className="d-flex vh-100 align-items-center justify-content-center bg-canvas">
        <div className="d-flex flex-column align-items-center gap-3">
          <Spinner animation="border" className="text-primary" />
          <p className="mb-0 fs-xs text-body-secondary">
            Đang xác thực quyền Quản trị viên...
          </p>
        </div>
      </div>
    );
  }

  // Once authenticated with ROLE_ADMIN, render the full admin menu and layout
  if (state === 'ok') {
    return children;
  }

  // Giao diện Modal Đăng Nhập Quản Trị Viên riêng biệt tại URL Admin (luôn dùng nền tối)
  return (
    <div
      data-bs-theme="dark"
      className="position-relative d-flex min-vh-100 align-items-center justify-content-center p-3 bg-black text-body"
    >
      {/* Background ambient pattern & glowing spotlight */}
      <div
        className="pe-none position-absolute top-0 start-0 w-100 h-100"
        style={{
          backgroundImage: 'radial-gradient(#FFA116 1px, transparent 1px)',
          backgroundSize: '24px 24px',
          opacity: 0.1,
        }}
      />
      <div
        className="pe-none position-absolute top-50 start-50 translate-middle rounded-circle"
        style={{ width: 500, height: 500, background: 'rgba(255, 161, 22, 0.1)', filter: 'blur(120px)' }}
      />

      {/* Admin Login Modal Container */}
      <div className="position-relative w-100 rounded-4 border bg-surface p-4 shadow-lg" style={{ maxWidth: 448 }}>
        {/* Header Branding */}
        <div className="text-center">
          <div
            className="mx-auto mb-3 d-flex align-items-center justify-content-center rounded-4 border border-primary border-opacity-25 bg-primary bg-opacity-10 text-primary"
            style={{ width: 52, height: 52 }}
          >
            <Shield size={28} />
          </div>
          <div className="d-flex align-items-center justify-content-center gap-2">
            <DapcodeLogo size="sm" />
            <span className="fs-lg fw-bold text-body-emphasis">LiệtCode</span>
            <span className="badge rounded-1 px-2 border border-primary border-opacity-25 bg-primary bg-opacity-25 text-primary text-uppercase fs-2xs fw-bold">
              Admin Portal
            </span>
          </div>
          <h2 className="mt-2 mb-0 fs-base fw-semibold text-body-emphasis">
            Đăng nhập Quản trị viên
          </h2>
          <p className="mt-1 mb-0 fs-xs text-body-secondary">
            Khu vực quản lý bài tập, bài học lý thuyết, test case và danh sách học viên.
          </p>
        </div>

        {/* Warning if logged in as regular student */}
        {currentUser && currentUser.role !== 'ROLE_ADMIN' && (
          <div className="alert alert-danger mt-3 mb-0 p-3 rounded-3 fs-xs">
            <div className="d-flex align-items-start gap-2">
              <ShieldAlert size={16} className="flex-shrink-0" />
              <div>
                <div>
                  Đang đăng nhập với: <strong>{currentUser.username || currentUser.email}</strong>
                </div>
                <div className="mt-1 fs-2xs opacity-75">
                  Tài khoản này có vai trò học viên (<code className="font-monospace text-reset">ROLE_USER</code>) và không thể vào trang Quản trị.
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={handleLogoutOtherAccount}
              className="btn btn-sm btn-outline-danger mt-2 w-100 d-flex align-items-center justify-content-center gap-2 fs-xs"
            >
              <LogOut size={14} />
              <span>Đăng xuất tài khoản này để đăng nhập Admin</span>
            </button>
          </div>
        )}

        {/* Error message */}
        {errorMessage && (
          <div className="alert alert-danger mt-3 mb-0 p-3 rounded-3 fs-xs d-flex align-items-start gap-2">
            <ShieldAlert size={16} className="flex-shrink-0" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Username & Password Form */}
        <form onSubmit={handlePasswordLogin} className="mt-4 d-flex flex-column gap-3">
          <div>
            <label className="form-label">
              Tài khoản / Email Quản trị
            </label>
            <div className="position-relative">
              <div className="pe-none position-absolute top-50 translate-middle-y d-flex text-body-secondary" style={{ left: 12 }}>
                <User size={16} />
              </div>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="admin hoặc admin@dapcode.io"
                className="form-control ps-5 py-2 rounded-3 fs-xs"
              />
            </div>
          </div>

          <div>
            <div className="d-flex align-items-center justify-content-between mb-1">
              <label className="form-label mb-0">
                Mật khẩu
              </label>
              <button
                type="button"
                onClick={handleFillDemo}
                className="btn btn-link p-0 border-0 fs-2xs text-primary d-flex align-items-center gap-1"
              >
                <Sparkles size={12} />
                <span>Điền mẫu (admin / admin123)</span>
              </button>
            </div>
            <div className="position-relative">
              <div className="pe-none position-absolute top-50 translate-middle-y d-flex text-body-secondary" style={{ left: 12 }}>
                <Lock size={16} />
              </div>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="form-control ps-5 pe-5 py-2 rounded-3 fs-xs"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="icon-btn position-absolute top-50 end-0 translate-middle-y me-1"
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isLoading}
            className="btn btn-primary mt-2 w-100 d-flex align-items-center justify-content-center gap-2 py-2 rounded-3 fs-xs fw-bold text-dark shadow-sm"
          >
            {isLoading ? <Spinner animation="border" size="sm" /> : <KeyRound size={16} />}
            <span>Đăng nhập vào Menu Admin</span>
          </button>
        </form>

        {/* Separator */}
        <div className="my-3 d-flex align-items-center gap-2">
          <div className="flex-grow-1 border-top" />
          <span className="fs-2xs fw-semibold text-uppercase text-nowrap text-body-secondary">
            hoặc phương thức khác
          </span>
          <div className="flex-grow-1 border-top" />
        </div>

        {/* Google OAuth & Quick Dev Mode */}
        <div className="d-flex flex-column gap-2">
          <button
            type="button"
            disabled={isLoading}
            onClick={handleGoogleLogin}
            className="btn btn-ghost border w-100 d-flex align-items-center justify-content-center gap-3 rounded-3 fs-xs"
          >
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
            <span>Đăng nhập Google (Tài khoản đã phân quyền Admin)</span>
          </button>


        </div>

        {/* Back to student page link */}
        <div className="mt-4 border-top pt-3 text-center">
          <Link
            to="/roadmap"
            className="d-inline-flex align-items-center gap-2 fs-xs link-body-emphasis link-opacity-50 link-opacity-100-hover text-decoration-none"
          >
            <ArrowLeft size={14} />
            <span>Quay lại trang học viên (Roadmap)</span>
          </Link>
        </div>
      </div>
    </div>
  );
}
