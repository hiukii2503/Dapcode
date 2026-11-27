import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import { AlertCircle, CheckCircle2 } from 'lucide-react';
import { loginWithGoogle, getCurrentUser } from '../api/auth';

const getSavedReturnUrl = () => {
  const raw =
    sessionStorage.getItem('dapcode_auth_return') ||
    localStorage.getItem('dapcode_auth_return') ||
    '';
  if (raw.startsWith('/') && !raw.startsWith('//') && !raw.startsWith('/auth/callback')) {
    return raw;
  }
  return '/roadmap';
};

const clearSavedReturnUrl = () => {
  sessionStorage.removeItem('dapcode_auth_return');
  localStorage.removeItem('dapcode_auth_return');
};

export default function AuthCallbackPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [status, setStatus] = useState('loading'); // 'loading' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('');
  const returnUrlRef = useRef(getSavedReturnUrl());

  useEffect(() => {
    let cancelled = false;
    let redirectTimer = null;

    const redirectBack = (delayMs = 0) => {
      const targetUrl = returnUrlRef.current || '/roadmap';
      const go = () => {
        if (cancelled) return;
        clearSavedReturnUrl();
        window.dispatchEvent(new Event('dapcode-auth-change'));
        navigate(targetUrl, { replace: true });
      };
      if (delayMs > 0) {
        redirectTimer = setTimeout(go, delayMs);
      } else {
        go();
      }
    };

    const handleCallback = async () => {
      try {
        const searchParams = new URLSearchParams(location.search);
        const isBanned = searchParams.get('banned') === 'true';
        if (isBanned) {
          const reason =
            searchParams.get('reason') ||
            'Vi phạm quy định và điều khoản sử dụng của hệ thống.';
          const email = searchParams.get('email') || null;
          localStorage.removeItem('dapcode_token');
          localStorage.removeItem('dapcode_user');
          sessionStorage.setItem(
            'dapcode_ban_info',
            JSON.stringify({ reason, email })
          );
          redirectBack(0);
          window.dispatchEvent(
            new CustomEvent('dapcode-user-banned', { detail: { reason, email } })
          );
          return;
        }

        const jwtToken = searchParams.get('token');
        if (jwtToken) {
          localStorage.setItem('dapcode_token', jwtToken);
          try {
            const userRes = await getCurrentUser();
            let userData = userRes?.data || userRes;
            if (userData && userData.data && typeof userData.data === 'object') {
              userData = userData.data;
            }
            if (userData) {
              if (!userData.name && userData.username) userData.name = userData.username;
              localStorage.setItem('dapcode_user', JSON.stringify(userData));
            }
          } catch (e) {
            console.warn('Không thể tải profile ngay:', e);
          }
          if (cancelled) return;
          setStatus('success');
          redirectBack(500);
          return;
        }

        let idToken = null;

        if (location.hash) {
          const hashParams = new URLSearchParams(location.hash.replace(/^#/, ''));
          idToken = hashParams.get('id_token');
        }

        if (!idToken && searchParams.has('id_token')) {
          idToken = searchParams.get('id_token');
        }

        if (!idToken) {
          throw new Error('Không tìm thấy Google ID Token trong URL phản hồi.');
        }

        const res = await loginWithGoogle(idToken);
        if (cancelled) return;
        const data = (res && res.data && res.data.token) ? res.data : (res?.data || res);

        if (data && data.token) {
          localStorage.setItem('dapcode_token', data.token);
          if (data.user) {
            const userObj = { ...data.user };
            if (!userObj.name && userObj.username) userObj.name = userObj.username;
            localStorage.setItem('dapcode_user', JSON.stringify(userObj));
          }
          setStatus('success');
          redirectBack(600);
        } else {
          throw new Error('Phản hồi từ máy chủ không hợp lệ.');
        }
      } catch (err) {
        if (cancelled) return;
        console.error('Lỗi xác thực OAuth:', err);
        if (err.response?.data?.code === 1008) {
          const reason =
            err.response?.data?.message ||
            'Vi phạm quy định và điều khoản sử dụng của hệ thống.';
          localStorage.removeItem('dapcode_token');
          localStorage.removeItem('dapcode_user');
          sessionStorage.setItem(
            'dapcode_ban_info',
            JSON.stringify({ reason })
          );
          redirectBack(0);
          window.dispatchEvent(
            new CustomEvent('dapcode-user-banned', { detail: { reason } })
          );
          return;
        }
        setStatus('error');
        setErrorMessage(
          err.response?.data?.message || err.message || 'Xác thực Google thất bại. Vui lòng thử lại.'
        );
      }
    };

    handleCallback();

    return () => {
      cancelled = true;
      if (redirectTimer) clearTimeout(redirectTimer);
    };
  }, [location, navigate]);

  const isReturningToProblem = returnUrlRef.current?.startsWith('/problems/');

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-canvas p-3">
      <div className="card w-100 p-4 text-center" style={{ maxWidth: 448 }}>
        {status === 'loading' && (
          <div className="d-flex flex-column align-items-center gap-3 py-2">
            <Spinner animation="border" className="text-body-secondary" />
            <h2 className="fs-sm fw-semibold text-body-emphasis mb-0">
              Đang xác thực tài khoản Google...
            </h2>
            <p className="fs-xs text-body-secondary mb-0">
              Vui lòng đợi giây lát trong khi LiệtCode đồng bộ tài khoản của bạn.
            </p>
          </div>
        )}

        {status === 'success' && (
          <div className="d-flex flex-column align-items-center gap-3 py-2">
            <div
              className="d-flex align-items-center justify-content-center rounded-circle bg-success-subtle text-success border border-success-subtle"
              style={{ width: 48, height: 48 }}
            >
              <CheckCircle2 size={24} />
            </div>
            <h2 className="fs-sm fw-semibold text-body-emphasis mb-0">Đăng nhập thành công!</h2>
            <p className="fs-xs text-body-secondary mb-0">
              {isReturningToProblem
                ? 'Đang quay lại bài tập bạn đang làm...'
                : 'Đang chuyển hướng về trang học tập...'}
            </p>
          </div>
        )}

        {status === 'error' && (
          <div className="d-flex flex-column align-items-center gap-3 py-2">
            <div
              className="d-flex align-items-center justify-content-center rounded-circle bg-danger-subtle text-danger border border-danger-subtle"
              style={{ width: 48, height: 48 }}
            >
              <AlertCircle size={24} />
            </div>
            <h2 className="fs-sm fw-semibold text-danger mb-0">
              Đăng nhập thất bại
            </h2>
            <p className="fs-xs text-body-secondary mb-0">{errorMessage}</p>
            <button
              type="button"
              onClick={() => {
                const targetUrl = returnUrlRef.current || '/roadmap';
                clearSavedReturnUrl();
                navigate(targetUrl, { replace: true });
              }}
              className="btn btn-sm btn-contrast mt-1 px-3 py-2 fs-xs"
            >
              {isReturningToProblem ? 'Quay lại bài tập' : 'Quay lại trang chủ'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
