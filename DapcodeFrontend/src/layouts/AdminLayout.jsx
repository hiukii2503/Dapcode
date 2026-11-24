import React from 'react';
import { NavLink, Link, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  FileCode2,
  FlaskConical,
  BookOpen,
  Users,
  ArrowLeft,
  Sun,
  Moon,
  LogOut,
} from 'lucide-react';
import DapcodeLogo from '../components/DapcodeLogo';
import { ToastProvider } from '../components/admin/AdminUi';
import { useTheme } from '../context/ThemeContext';

const NAV = [
  { to: '/admin', label: 'Tổng quan', icon: LayoutDashboard, end: true },
  { to: '/admin/problems', label: 'Bài tập', icon: FileCode2 },
  { to: '/admin/testcases', label: 'Test case', icon: FlaskConical },
  { to: '/admin/theories', label: 'Bài giảng', icon: BookOpen },
  { to: '/admin/users', label: 'Học viên', icon: Users },
];

export default function AdminLayout() {
  const { isDark, toggleTheme } = useTheme();
  const navigate = useNavigate();

  let me = null;
  try {
    me = JSON.parse(localStorage.getItem('dapcode_user') || 'null');
  } catch {
    me = null;
  }
  const name = me?.username || me?.name || me?.email?.split('@')[0] || 'Admin';

  const logout = () => {
    localStorage.removeItem('dapcode_user');
    localStorage.removeItem('dapcode_token');
    navigate('/admin');
    window.location.reload();
  };

  const linkCls = ({ isActive }) => `side-link ${isActive ? 'active' : ''}`;

  return (
    <ToastProvider>
      <div className="d-flex min-vh-100 bg-canvas">
        <aside className="sticky-top d-flex flex-column flex-shrink-0 vh-100 border-end bg-surface" style={{ maxWidth: 240 }}>
          {/* Forces the full 240px width from md up; below md the sidebar collapses to an icon rail */}
          <div className="d-none d-md-block" style={{ width: 239 }} />

          <Link to="/admin" className="app-header d-flex align-items-center gap-2 border-bottom px-3 text-decoration-none text-body-emphasis">
            <DapcodeLogo size="sm" />
            <div className="d-none d-md-flex align-items-baseline gap-2">
              <span className="fs-base fw-bold">LiệtCode</span>
              <span className="badge rounded-1 px-2 bg-primary bg-opacity-25 text-primary text-uppercase fs-2xs fw-semibold">
                Admin
              </span>
            </div>
          </Link>

          <nav className="flex-grow-1 d-flex flex-column gap-1 p-2">
            {NAV.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink key={item.to} to={item.to} end={item.end} className={linkCls} title={item.label}>
                  <Icon size={18} className="flex-shrink-0" />
                  <span className="d-none d-md-inline">{item.label}</span>
                </NavLink>
              );
            })}
          </nav>

          <div className="d-flex flex-column gap-1 border-top p-2">
            <Link to="/roadmap" className={linkCls({ isActive: false })} title="Về trang học viên">
              <ArrowLeft size={18} className="flex-shrink-0" />
              <span className="d-none d-md-inline">Trang học viên</span>
            </Link>
            <button type="button" onClick={toggleTheme} className={`${linkCls({ isActive: false })} cursor-pointer`}>
              {isDark ? <Sun size={18} className="flex-shrink-0" /> : <Moon size={18} className="flex-shrink-0" />}
              <span className="d-none d-md-inline">{isDark ? 'Chế độ sáng' : 'Chế độ tối'}</span>
            </button>
            <div className="d-flex align-items-center gap-2 rounded-2 px-3 py-1">
              <div className="avatar avatar-sm">
                {me?.avatarUrl ? <img src={me.avatarUrl} alt={name} referrerPolicy="no-referrer" /> : name.charAt(0).toUpperCase()}
              </div>
              <span className="d-none d-md-inline flex-grow-1 min-w-0 text-truncate">{name}</span>
              <button type="button" onClick={logout} title="Đăng xuất" className="icon-btn icon-btn-danger d-none d-md-inline-flex">
                <LogOut size={16} />
              </button>
            </div>
          </div>
        </aside>

        <main className="flex-grow-1 min-w-0 p-4">
          <div className="mx-auto" style={{ maxWidth: 1200 }}>
            <Outlet />
          </div>
        </main>
      </div>
    </ToastProvider>
  );
}
