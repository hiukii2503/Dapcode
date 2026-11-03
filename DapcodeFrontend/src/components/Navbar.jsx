import React, { useState, useEffect } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Dropdown } from 'react-bootstrap';
import { Search, Flame, Sun, Moon, LogOut, User, Shield } from 'lucide-react';
import SearchModal from './SearchModal';
import AuthModal from './AuthModal';
import StreakModal from './StreakModal';
import DapcodeLogo from './DapcodeLogo';
import { useTheme } from '../context/ThemeContext';

const NAV_LINKS = [
  { label: 'Roadmap', path: '/roadmap' },
  { label: 'Problems', path: '/practice', alt: '/problems' },
  { label: 'Theories', path: '/theories' },
];

export default function Navbar() {
  const location = useLocation();
  const { isDark, toggleTheme } = useTheme();

  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [isStreakOpen, setIsStreakOpen] = useState(false);

  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('dapcode_user') || 'null');
    } catch {
      return null;
    }
  });

  useEffect(() => {
    const handleKeyDown = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault();
        setIsSearchOpen((prev) => !prev);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('dapcode_user');
    localStorage.removeItem('dapcode_token');
    setUser(null);
  };

  const displayName = user?.username || user?.name || user?.email?.split('@')[0] || 'User';
  const displayStreak = user?.streakCount ?? user?.streak ?? 0;
  const initialLetter = displayName.charAt(0).toUpperCase();

  return (
    <>
      <header className="app-header sticky-top w-100 border-bottom bg-surface">
        <div className="app-container mx-auto h-100 d-flex align-items-center justify-content-between px-3">
          <div className="d-flex align-items-center gap-4">
            <Link to="/roadmap" className="d-flex align-items-center gap-2 text-decoration-none">
              <DapcodeLogo size="sm" />
              <span className="fs-base fw-bold text-body-emphasis d-none d-sm-inline">LiệtCode</span>
            </Link>

            <nav className="app-nav nav d-none d-md-flex">
              {NAV_LINKS.map((item) => {
                const isActive = location.pathname === item.path || location.pathname === item.alt;
                return (
                  <Link key={item.label} to={item.path} className={`nav-link ${isActive ? 'active' : ''}`}>
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>

          <div className="d-flex align-items-center gap-1">
            <button type="button" onClick={() => setIsSearchOpen(true)} className="icon-btn" title="Tìm kiếm (Ctrl+K)">
              <Search size={18} />
            </button>

            <button type="button" onClick={() => setIsStreakOpen(true)} className="icon-btn w-auto px-2" title="Chuỗi streak">
              <Flame size={18} className="text-primary" />
              <span className="fw-medium">{displayStreak}</span>
            </button>

            <button type="button" onClick={toggleTheme} className="icon-btn" title={isDark ? 'Chế độ sáng' : 'Chế độ tối'}>
              {isDark ? <Sun size={18} /> : <Moon size={18} />}
            </button>

            {user ? (
              <Dropdown align="end" className="ms-2">
                <Dropdown.Toggle as="button" bsPrefix="avatar cursor-pointer" title={displayName}>
                  {user.avatarUrl ? <img src={user.avatarUrl} alt={displayName} referrerPolicy="no-referrer" /> : initialLetter}
                </Dropdown.Toggle>

                <Dropdown.Menu className="shadow py-1">
                  <div className="px-3 py-2 border-bottom">
                    <div className="fw-medium text-body-emphasis text-truncate">{displayName}</div>
                    <div className="fs-xs text-body-secondary text-truncate">{user.email}</div>
                  </div>
                  <div className="d-flex align-items-center justify-content-between px-3 py-2 fs-xs text-body-secondary">
                    <span>XP</span>
                    <span className="fw-medium text-body-emphasis">{user.xp ?? 0}</span>
                  </div>
                  {user.role === 'ROLE_ADMIN' && (
                    <Dropdown.Item as={Link} to="/admin" className="d-flex align-items-center gap-2 text-primary fw-medium">
                      <Shield size={16} />
                      <span>Trang Quản trị (Admin)</span>
                    </Dropdown.Item>
                  )}
                  <Dropdown.Item as="button" onClick={handleLogout} className="d-flex align-items-center gap-2 text-body-secondary">
                    <LogOut size={16} />
                    <span>Đăng xuất</span>
                  </Dropdown.Item>
                </Dropdown.Menu>
              </Dropdown>
            ) : (
              <button type="button" onClick={() => setIsAuthOpen(true)} className="icon-btn w-auto px-3 ms-1 fw-medium">
                <User size={16} />
                <span>Sign In</span>
              </button>
            )}
          </div>
        </div>
      </header>

      <SearchModal isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)} />
      <AuthModal isOpen={isAuthOpen} onClose={() => setIsAuthOpen(false)} onLoginSuccess={(u) => setUser(u)} />
      <StreakModal isOpen={isStreakOpen} onClose={() => setIsStreakOpen(false)} streakCount={displayStreak} />
    </>
  );
}
