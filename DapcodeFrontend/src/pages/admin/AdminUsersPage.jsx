import React, { useEffect, useState, useCallback } from 'react';
import { Search, Shield, ShieldAlert, UserCheck, UserX, AlertTriangle } from 'lucide-react';
import { adminGetUsers, setUserStatus, setUserRole, errMsg } from '../../api/admin';
import {
  Card,
  PageHeader,
  Dialog,
  ConfirmDialog,
  Pagination,
  Spinner,
  Empty,
  Badge,
  useToast,
  useDebounced,
  inputCls,
  selectCls,
  btnPrimary,
  btnGhost,
  btnDanger,
  iconBtn,
} from '../../components/admin/AdminUi';

const thCls = 'fw-medium text-body-secondary';

function StatusDialog({ open, user, onClose, onSaved }) {
  const toast = useToast();
  const [bannedReason, setBannedReason] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) setBannedReason('');
  }, [open]);

  if (!user) return null;
  const isBanning = user.active;

  const handleSubmit = async () => {
    if (isBanning && !bannedReason.trim()) {
      toast.push('error', 'Vui lòng nhập lý do khóa tài khoản');
      return;
    }
    setSaving(true);
    try {
      await setUserStatus(user.id, !isBanning, isBanning ? bannedReason.trim() : null);
      toast.push('success', isBanning ? 'Đã khóa tài khoản thành công' : 'Đã mở khóa tài khoản');
      onSaved();
    } catch (e) {
      toast.push('error', errMsg(e, 'Thao tác không thành công'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isBanning ? 'Khóa tài khoản học viên' : 'Mở khóa tài khoản học viên'}
      size="sm"
    >
      <div className="d-flex flex-column gap-3">
        <p className="mb-0 text-body-secondary">
          Bạn đang {isBanning ? 'khóa' : 'mở khóa'} tài khoản:{' '}
          <strong className="text-body-emphasis">
            {user.username} ({user.email})
          </strong>
        </p>

        {isBanning && (
          <div>
            <label className="form-label d-block">
              Lý do khóa tài khoản <span className="text-danger">*</span>
            </label>
            <textarea
              value={bannedReason}
              onChange={(e) => setBannedReason(e.target.value)}
              placeholder="VD: Vi phạm điều khoản sử dụng, spam bài nộp..."
              rows={3}
              className={inputCls}
            />
          </div>
        )}

        <div className="d-flex justify-content-end gap-2 border-top pt-3">
          <button type="button" onClick={onClose} className={btnGhost} disabled={saving}>
            Hủy
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            className={isBanning ? btnDanger : btnPrimary}
            disabled={saving}
          >
            {saving ? 'Đang xử lý...' : isBanning ? 'Khóa tài khoản' : 'Mở khóa'}
          </button>
        </div>
      </div>
    </Dialog>
  );
}

export default function AdminUsersPage() {
  const toast = useToast();
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounced(search, 300);
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [selectedUser, setSelectedUser] = useState(null);
  const [statusOpen, setStatusOpen] = useState(false);
  const [roleChangeUser, setRoleChangeUser] = useState(null);
  const [roleChanging, setRoleChanging] = useState(false);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: 15,
        search: debouncedSearch.trim() || undefined,
        role: roleFilter !== 'ALL' ? roleFilter : undefined,
        isActive: statusFilter === 'ACTIVE' ? true : statusFilter === 'BANNED' ? false : undefined,
      };
      const res = await adminGetUsers(params);
      setUsers(res?.content || []);
      setTotalPages(res?.totalPages || 1);
      setTotalElements(res?.totalElements || 0);
    } catch (e) {
      toast.push('error', errMsg(e, 'Không tải được danh sách người dùng'));
    } finally {
      setLoading(false);
    }
  }, [page, debouncedSearch, roleFilter, statusFilter, toast]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const handleRoleToggle = async () => {
    if (!roleChangeUser) return;
    const newRole = roleChangeUser.role === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';
    setRoleChanging(true);
    try {
      await setUserRole(roleChangeUser.id, newRole);
      toast.push('success', `Đã đổi vai trò sang ${newRole}`);
      setRoleChangeUser(null);
      loadUsers();
    } catch (e) {
      toast.push('error', errMsg(e, 'Không thể cập nhật vai trò'));
    } finally {
      setRoleChanging(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Quản lý học viên & tài khoản"
        subtitle="Tra cứu tài khoản, phân quyền quản trị viên và xử lý vi phạm học viên."
      />

      <Card className="p-3 mb-4">
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div className="d-flex flex-grow-1 flex-wrap align-items-center gap-2">
            <div className="position-relative flex-grow-1" style={{ minWidth: 240 }}>
              <Search size={16} className="position-absolute top-50 translate-middle-y text-body-secondary" style={{ left: 12 }} />
              <input
                type="text"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                placeholder="Tìm theo username hoặc email..."
                className={`${inputCls} ps-5`}
              />
            </div>
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value);
                setPage(0);
              }}
              className={`${selectCls} w-auto`}
            >
              <option value="ALL">Tất cả vai trò</option>
              <option value="ROLE_USER">Học viên (User)</option>
              <option value="ROLE_ADMIN">Quản trị viên (Admin)</option>
            </select>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className={`${selectCls} w-auto`}
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="BANNED">Đã bị khóa</option>
            </select>
          </div>
          <span className="fs-xs text-body-secondary">
            Tổng cộng: <strong className="text-body-emphasis">{totalElements}</strong> tài khoản
          </span>
        </div>
      </Card>

      <Card className="overflow-hidden">
        {loading ? (
          <div className="py-5 text-center">
            <Spinner />
          </div>
        ) : users.length === 0 ? (
          <div className="py-5">
            <Empty>
              <div className="fw-medium text-body-emphasis">Không tìm thấy tài khoản</div>
              <div className="fs-xs">Thử đổi từ khóa hoặc bộ lọc tìm kiếm.</div>
            </Empty>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="bg-body-tertiary fs-xs">
                <tr>
                  <th className={`ps-3 ${thCls}`}>Học viên</th>
                  <th className={thCls}>Email</th>
                  <th className={thCls}>Vai trò</th>
                  <th className={thCls}>Trạng thái</th>
                  <th className={thCls}>Ngày tham gia</th>
                  <th className={`pe-3 text-end ${thCls}`}>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => {
                  const isAdmin = u.role === 'ROLE_ADMIN';
                  return (
                    <tr key={u.id}>
                      <td className="ps-3">
                        <div className="d-flex align-items-center gap-3">
                          <div className="avatar fs-xs">
                            {u.avatarUrl ? (
                              <img src={u.avatarUrl} alt={u.username} referrerPolicy="no-referrer" />
                            ) : (
                              (u.username || 'U').charAt(0).toUpperCase()
                            )}
                          </div>
                          <div>
                            <div className="fw-medium text-body-emphasis">{u.username}</div>
                            {u.bannedReason && (
                              <div className="d-flex align-items-center gap-1 fs-2xs text-danger">
                                <AlertTriangle size={12} />
                                <span>{u.bannedReason}</span>
                              </div>
                            )}
                          </div>
                        </div>
                      </td>
                      <td className="fs-xs text-body-secondary">{u.email}</td>
                      <td>
                        {isAdmin ? (
                          <Badge tone="orange">
                            <Shield size={12} className="me-1 align-text-bottom" /> ADMIN
                          </Badge>
                        ) : (
                          <Badge tone="neutral">USER</Badge>
                        )}
                      </td>
                      <td>
                        {u.active ? (
                          <Badge tone="green">Hoạt động</Badge>
                        ) : (
                          <Badge tone="red">Bị khóa</Badge>
                        )}
                      </td>
                      <td className="fs-xs text-body-secondary">
                        {u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '—'}
                      </td>
                      <td className="pe-3 text-end">
                        <div className="d-flex align-items-center justify-content-end gap-1">
                          <button
                            type="button"
                            onClick={() => setRoleChangeUser(u)}
                            className={iconBtn}
                            title={isAdmin ? 'Hạ quyền về User' : 'Nâng cấp quyền Admin'}
                          >
                            {isAdmin ? <ShieldAlert size={16} className="text-primary" /> : <Shield size={16} />}
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setSelectedUser(u);
                              setStatusOpen(true);
                            }}
                            className={`${iconBtn} ${u.active ? 'icon-btn-danger' : 'icon-btn-success'}`}
                            title={u.active ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
                          >
                            {u.active ? <UserX size={16} /> : <UserCheck size={16} />}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </Card>

      <StatusDialog
        open={statusOpen}
        user={selectedUser}
        onClose={() => setStatusOpen(false)}
        onSaved={() => {
          setStatusOpen(false);
          loadUsers();
        }}
      />

      <ConfirmDialog
        open={Boolean(roleChangeUser)}
        title="Thay đổi vai trò người dùng"
        message={`Bạn có chắc muốn đổi vai trò của "${roleChangeUser?.username}" từ ${
          roleChangeUser?.role === 'ROLE_ADMIN' ? 'ADMIN sang USER' : 'USER sang ADMIN'
        }?`}
        confirmLabel="Đổi vai trò"
        busy={roleChanging}
        onConfirm={handleRoleToggle}
        onClose={() => setRoleChangeUser(null)}
      />
    </div>
  );
}
