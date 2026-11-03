import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Users, FileCode2, Send, CheckCircle2 } from 'lucide-react';
import { getDashboardStats, errMsg } from '../../api/admin';
import { Card, PageHeader, Spinner, Badge } from '../../components/admin/AdminUi';

const LANGS = { 71: 'Python', 62: 'Java', 54: 'C++' };

const statusTone = (s) => (s === 'ACCEPTED' ? 'green' : s === 'PENDING' || s === 'RUNNING' ? 'orange' : 'red');

const timeAgo = (iso) => {
  if (!iso) return '';
  const diff = Math.max(0, Date.now() - new Date(iso).getTime());
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'vừa xong';
  if (m < 60) return `${m} phút trước`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h} giờ trước`;
  return `${Math.floor(h / 24)} ngày trước`;
};

function Kpi({ icon: Icon, label, value, sub, color }) {
  return (
    <Card className="p-3 h-100">
      <div className="d-flex align-items-start justify-content-between">
        <div>
          <div className="text-body-secondary">{label}</div>
          <div className="mt-1 fs-2xl fw-semibold text-body-emphasis">{value}</div>
          <div className="mt-1 fs-xs text-body-secondary">{sub}</div>
        </div>
        <div
          className="d-flex align-items-center justify-content-center rounded-2"
          style={{ width: 36, height: 36, background: `${color}1A`, color }}
        >
          <Icon size={18} />
        </div>
      </div>
    </Card>
  );
}

function BarChart({ data }) {
  const max = Math.max(1, ...data.map((d) => d.count));
  return (
    <div className="d-flex gap-3" style={{ height: 176 }}>
      {data.map((d) => (
        <div key={d.date} className="d-flex flex-grow-1 flex-column align-items-center gap-2">
          <span className="fs-xs text-body-secondary">{d.count}</span>
          <div className="d-flex w-100 flex-grow-1 min-h-0 align-items-end">
            <div
              className="w-100 rounded-top-1 bg-primary"
              style={{
                height: `${(d.count / max) * 100}%`,
                minHeight: d.count ? 4 : 2,
                opacity: d.count ? 1 : 0.25,
                transition: 'height .5s ease',
              }}
            />
          </div>
          <span className="fs-2xs text-body-secondary">{d.date.slice(5)}</span>
        </div>
      ))}
    </div>
  );
}

export default function AdminDashboardPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch((e) => setError(errMsg(e, 'Không tải được số liệu thống kê')));
  }, []);

  if (error) return <Card className="p-4 text-danger">{error}</Card>;
  if (!stats) return <Spinner label="Đang tải số liệu..." />;

  const diff = [
    { label: 'Easy', value: stats.easyProblems, color: '#00B8A3' },
    { label: 'Medium', value: stats.mediumProblems, color: '#FFB800' },
    { label: 'Hard', value: stats.hardProblems, color: '#FF375F' },
  ];
  const diffMax = Math.max(1, stats.totalProblems);

  return (
    <>
      <PageHeader title="Tổng quan" subtitle="Số liệu hoạt động của hệ thống LiệtCode." />

      <div className="row g-3">
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi icon={Users} label="Học viên" value={stats.totalUsers} sub={`${stats.activeUsers} hoạt động · ${stats.bannedUsers} bị khóa`} color="#3B82F6" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi icon={FileCode2} label="Bài tập" value={stats.totalProblems} sub={`${stats.easyProblems} dễ · ${stats.mediumProblems} vừa · ${stats.hardProblems} khó`} color="#FFA116" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi icon={Send} label="Lượt nộp bài" value={stats.totalSubmissions} sub={`${stats.totalDailySubmissions} trong 7 ngày qua`} color="#8B5CF6" />
        </div>
        <div className="col-12 col-sm-6 col-xl-3">
          <Kpi icon={CheckCircle2} label="Tỷ lệ Accepted" value={`${stats.acceptedRate}%`} sub="Trên tổng số lượt nộp" color="#2CBB5D" />
        </div>

        <div className="col-12 col-lg-8">
          <Card className="p-4 h-100">
            <div className="mb-3 d-flex align-items-center justify-content-between">
              <h2 className="mb-0 fs-sm fw-semibold">Lượt nộp bài 7 ngày gần nhất</h2>
              <span className="fs-xs text-body-secondary">{stats.totalDailySubmissions} lượt</span>
            </div>
            <BarChart data={stats.submissionLast7Days || []} />
          </Card>
        </div>

        <div className="col-12 col-lg-4">
          <Card className="p-4 h-100">
            <h2 className="mb-3 fs-sm fw-semibold">Bài tập theo độ khó</h2>
            <div className="d-flex flex-column gap-3">
              {diff.map((d) => (
                <div key={d.label}>
                  <div className="mb-1 d-flex justify-content-between">
                    <span style={{ color: d.color }} className="fw-medium">
                      {d.label}
                    </span>
                    <span className="text-body-secondary">{d.value}</span>
                  </div>
                  <div className="progress" style={{ height: 8 }}>
                    <div className="progress-bar" style={{ width: `${(d.value / diffMax) * 100}%`, background: d.color }} />
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>
      </div>

      <Card className="mt-3">
        <div className="d-flex align-items-center justify-content-between px-4 py-3">
          <h2 className="mb-0 fs-sm fw-semibold">Bài nộp gần đây</h2>
        </div>
        <table className="table table-striped align-middle mb-0 border-top">
          <thead>
            <tr className="fs-xs">
              <th className="ps-4 text-body-secondary">Học viên</th>
              <th className="text-body-secondary">Bài tập</th>
              <th className="d-none d-sm-table-cell text-body-secondary">Ngôn ngữ</th>
              <th className="text-body-secondary">Kết quả</th>
              <th className="pe-4 text-end text-body-secondary">Thời gian</th>
            </tr>
          </thead>
          <tbody>
            {(stats.recentSubmission || []).map((s) => (
              <tr key={s.id}>
                <td className="ps-4">
                  <div className="d-flex align-items-center gap-2">
                    <div className="avatar avatar-sm">
                      {s.userAvatar ? <img src={s.userAvatar} alt="" referrerPolicy="no-referrer" /> : s.username?.charAt(0).toUpperCase()}
                    </div>
                    <span>{s.username}</span>
                  </div>
                </td>
                <td>
                  <Link to={`/problems/${s.problemSlug}`} className="link-body-emphasis text-decoration-none">
                    {s.problemTitle}
                  </Link>
                </td>
                <td className="d-none d-sm-table-cell text-body-secondary">{LANGS[s.languageId] || s.languageId}</td>
                <td>
                  <Badge tone={statusTone(s.status)}>{s.status}</Badge>
                </td>
                <td className="pe-4 text-end text-body-secondary">{timeAgo(s.submittedAt)}</td>
              </tr>
            ))}
            {!(stats.recentSubmission || []).length && (
              <tr>
                <td colSpan={5} className="py-5 text-center text-body-secondary">
                  Chưa có bài nộp nào.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </Card>
    </>
  );
}
