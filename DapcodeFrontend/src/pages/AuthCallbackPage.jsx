import React, { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Spinner } from 'react-bootstrap';
import { loginWithGoogle, getCurrentUser } from '../api/auth';
import { AlertCircle, CheckCircle2 } from 'lucide-react';

export default function AuthCallbackPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [status, setStatus] = useState('loading'); // 'loading' | 'success' | 'error'
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const searchParams = new URLSearchParams(location.search);
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
          setStatus('success');
          const returnUrl = sessionStorage.getItem('dapcode_auth_return') || '/roadmap';
          sessionStorage.removeItem('dapcode_auth_return');
          setTimeout(() => {
            navigate(returnUrl, { replace: true });
            window.location.reload();
          }, 600);
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
        const data = (res && res.data && res.data.token) ? res.data : (res?.data || res);

        if (data && data.token) {
          localStorage.setItem('dapcode_token', data.token);
          if (data.user) {
            const userObj = { ...data.user };
            if (!userObj.name && userObj.username) userObj.name = userObj.username;
            localStorage.setItem('dapcode_user', JSON.stringify(userObj));
          }
          setStatus('success');

          const returnUrl = sessionStorage.getItem('dapcode_auth_return') || '/roadmap';
          sessionStorage.removeItem('dapcode_auth_return');
          setTimeout(() => {
            navigate(returnUrl, { replace: true });
            window.location.reload();
          }, 800);
        } else {
          throw new Error('Phản hồi từ máy chủ không hợp lệ.');
        }
      } catch (err) {
        console.error('Lỗi xác thực OAuth:', err);
        setStatus('error');
        setErrorMessage(
          err.response?.data?.message || err.message || 'Xác thực Google thất bại. Vui lòng thử lại.'
        );
      }
    };

    handleCallback();
  }, [location, navigate]);

  const iconCircle = 'd-flex align-items-center justify-content-center rounded-circle border';

  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center p-3">
      <div className="w-100 rounded-2 border bg-surface p-5 text-center" style={{ maxWidth: 448 }}>
        {status === 'loading' && (
          <div className="d-flex flex-column align-items-center gap-3">
            <Spinner animation="border" />
            <h2 className="fs-sm fw-semibold mb-0">Đang xác thực tài khoản Google...</h2>
            <p className="fs-xs text-body-secondary mb-0">
              Vui lòng đợi giây lát trong khi LiệtCode đồng bộ tài khoản của bạn.
            </p>
          </div>
        )}

        {status === 'success' && (
          <div className="d-flex flex-column align-items-center gap-3">
            <div className={`${iconCircle} bg-success-subtle border-success-subtle text-success`} style={{ width: 48, height: 48 }}>
              <CheckCircle2 size={24} />
            </div>
            <h2 className="fs-sm fw-semibold mb-0">Đăng nhập thành công!</h2>
            <p className="fs-xs text-body-secondary mb-0">Đang chuyển hướng về trang học tập...</p>
          </div>
        )}

        {status === 'error' && (
          <div className="d-flex flex-column align-items-center gap-3">
            <div className={`${iconCircle} bg-danger-subtle border-danger-subtle text-danger`} style={{ width: 48, height: 48 }}>
              <AlertCircle size={24} />
            </div>
            <h2 className="fs-sm fw-semibold text-danger mb-0">Đăng nhập thất bại</h2>
            <p className="fs-xs text-body-secondary mb-0">{errorMessage}</p>
            <button
              onClick={() => navigate('/roadmap', { replace: true })}
              className="btn btn-sm btn-contrast mt-2"
            >
              Quay lại trang chủ
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
