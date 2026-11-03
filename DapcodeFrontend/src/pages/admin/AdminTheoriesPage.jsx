import React, { useEffect, useState, useCallback } from 'react';
import { Plus, Search, Pencil, Trash2, Eye, EyeOff } from 'lucide-react';
import {
  adminGetTheories,
  createTheory,
  updateTheory,
  deleteTheory,
  errMsg,
} from '../../api/admin';
import MarkdownRenderer from '../../components/MarkdownRenderer';
import {
  Card,
  PageHeader,
  Field,
  Dialog,
  ConfirmDialog,
  Toggle,
  Spinner,
  Empty,
  Badge,
  useToast,
  inputCls,
  selectCls,
  btnPrimary,
  btnGhost,
  iconBtn,
} from '../../components/admin/AdminUi';

const emptyForm = {
  title: '',
  slug: '',
  category: '',
  summary: '',
  content: '',
  orderIndex: 0,
  isPublished: true,
};

function slugify(text) {
  return text
    .toString()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/[đĐ]/g, 'd')
    .replace(/[^a-z0-9 -]/g, '')
    .trim()
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-');
}

function TheoryEditor({ open, theory, onClose, onSaved }) {
  const toast = useToast();
  const isEdit = Boolean(theory);
  const [form, setForm] = useState(emptyForm);
  const [tab, setTab] = useState('edit');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setTab('edit');
    if (theory) {
      setForm({
        title: theory.title || '',
        slug: theory.slug || '',
        category: theory.category || '',
        summary: theory.summary || '',
        content: theory.content || '',
        orderIndex: theory.orderIndex ?? 0,
        isPublished: theory.published ?? true,
      });
    } else {
      setForm(emptyForm);
    }
  }, [open, theory]);

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleTitleChange = (val) => {
    setForm((f) => ({
      ...f,
      title: val,
      slug: !isEdit && (!f.slug || f.slug === slugify(f.title)) ? slugify(val) : f.slug,
    }));
  };

  const save = async () => {
    if (!form.title.trim() || !form.content.trim()) {
      toast.push('error', 'Tiêu đề và nội dung bài học là bắt buộc');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        title: form.title.trim(),
        slug: (form.slug.trim() || slugify(form.title)).toLowerCase(),
        category: form.category.trim() || 'General',
        summary: form.summary.trim(),
        content: form.content,
        orderIndex: Number(form.orderIndex) || 0,
        isPublished: form.isPublished,
      };
      if (isEdit) {
        await updateTheory(theory.id, payload);
        toast.push('success', 'Đã cập nhật bài học');
      } else {
        await createTheory(payload);
        toast.push('success', 'Đã tạo bài học mới');
      }
      onSaved();
    } catch (e) {
      toast.push('error', errMsg(e, 'Không thể lưu bài học'));
    } finally {
      setSaving(false);
    }
  };

  const tabCls = (t) => `nav-link cursor-pointer ${tab === t ? 'active' : ''}`;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEdit ? 'Chỉnh sửa bài học lý thuyết' : 'Tạo bài học lý thuyết mới'}
      size="lg"
    >
      <div className="d-flex flex-column gap-3">
        <div className="row g-3">
          <div className="col-12 col-sm-6">
            <Field label="Tiêu đề" required>
              <input
                type="text"
                value={form.title}
                onChange={(e) => handleTitleChange(e.target.value)}
                placeholder="VD: Binary Search Toàn Tập"
                className={inputCls}
              />
            </Field>
          </div>
          <div className="col-12 col-sm-6">
            <Field label="Slug URL" required hint="Định danh duy nhất trên thanh địa chỉ">
              <input
                type="text"
                value={form.slug}
                onChange={(e) => set('slug', e.target.value)}
                placeholder="binary-search-toan-tap"
                className={inputCls}
              />
            </Field>
          </div>
        </div>

        <div className="row g-3">
          <div className="col-12 col-sm-4">
            <Field label="Chuyên mục" required>
              <input
                type="text"
                value={form.category}
                onChange={(e) => set('category', e.target.value)}
                placeholder="VD: Search, Dynamic Programming"
                className={inputCls}
              />
            </Field>
          </div>
          <div className="col-12 col-sm-4">
            <Field label="Thứ tự hiển thị (Order)">
              <input
                type="number"
                value={form.orderIndex}
                onChange={(e) => set('orderIndex', e.target.value)}
                className={inputCls}
              />
            </Field>
          </div>
          <div className="col-12 col-sm-4 d-flex align-items-center pt-sm-4">
            <Toggle
              checked={form.isPublished}
              onChange={(v) => set('isPublished', v)}
              label="Công khai cho học viên"
            />
          </div>
        </div>

        <Field label="Tóm tắt ngắn (Summary)">
          <textarea
            value={form.summary}
            onChange={(e) => set('summary', e.target.value)}
            rows={2}
            placeholder="Mô tả ngắn gọn nội dung bài học..."
            className={inputCls}
          />
        </Field>

        <div>
          <div className="mb-2 d-flex align-items-center justify-content-between">
            <span className="form-label mb-0">
              Nội dung bài học (Markdown) <span className="text-danger">*</span>
            </span>
            <div className="nav nav-pills nav-soft d-inline-flex bg-body-tertiary rounded-2 p-1 fs-xs">
              <button type="button" onClick={() => setTab('edit')} className={tabCls('edit')}>
                Soạn thảo
              </button>
              <button type="button" onClick={() => setTab('preview')} className={tabCls('preview')}>
                Xem trước
              </button>
            </div>
          </div>

          {tab === 'edit' ? (
            <textarea
              value={form.content}
              onChange={(e) => set('content', e.target.value)}
              rows={12}
              placeholder="# Hướng dẫn lý thuyết..."
              className={`${inputCls} font-monospace fs-xs lh-lg`}
            />
          ) : (
            <div className="overflow-y-auto rounded-2 border bg-surface p-3" style={{ minHeight: 280, maxHeight: 420 }}>
              <MarkdownRenderer content={form.content || '*Chưa có nội dung soạn thảo*'} />
            </div>
          )}
        </div>

        <div className="d-flex justify-content-end gap-2 border-top pt-3">
          <button type="button" onClick={onClose} className={btnGhost} disabled={saving}>
            Hủy
          </button>
          <button type="button" onClick={save} className={btnPrimary} disabled={saving}>
            {saving ? 'Đang lưu...' : isEdit ? 'Cập nhật' : 'Tạo mới'}
          </button>
        </div>
      </div>
    </Dialog>
  );
}

const thCls = 'bg-body-tertiary fw-medium';

export default function AdminTheoriesPage() {
  const toast = useToast();
  const [theories, setTheories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [editItem, setEditItem] = useState(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [deleteId, setDeleteId] = useState(null);
  const [deleting, setDeleting] = useState(false);

  const loadTheories = useCallback(async () => {
    setLoading(true);
    try {
      const data = await adminGetTheories();
      setTheories(Array.isArray(data) ? data : []);
    } catch (e) {
      toast.push('error', errMsg(e, 'Không tải được danh sách bài giảng'));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    loadTheories();
  }, [loadTheories]);

  const categories = ['ALL', ...Array.from(new Set(theories.map((t) => t.category).filter(Boolean)))];

  const filtered = theories.filter((t) => {
    if (categoryFilter !== 'ALL' && t.category !== categoryFilter) return false;
    if (search.trim()) {
      const q = search.toLowerCase();
      return (
        (t.title || '').toLowerCase().includes(q) ||
        (t.slug || '').toLowerCase().includes(q) ||
        (t.category || '').toLowerCase().includes(q)
      );
    }
    return true;
  });

  const confirmDelete = async () => {
    if (!deleteId) return;
    setDeleting(true);
    try {
      await deleteTheory(deleteId);
      toast.push('success', 'Đã xóa bài giảng');
      setDeleteId(null);
      loadTheories();
    } catch (e) {
      toast.push('error', errMsg(e, 'Không thể xóa bài giảng'));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <div className="d-flex flex-column gap-3">
      <PageHeader
        title="Quản lý bài giảng lý thuyết"
        subtitle="Thêm mới, cập nhật nội dung markdown và quản lý trạng thái hiển thị lý thuyết."
        actions={
          <button
            type="button"
            onClick={() => {
              setEditItem(null);
              setEditorOpen(true);
            }}
            className={btnPrimary}
          >
            <Plus size={16} />
            <span>Tạo bài học</span>
          </button>
        }
      />

      <Card className="p-3">
        <div className="d-flex flex-wrap align-items-center justify-content-between gap-3">
          <div className="d-flex flex-grow-1 align-items-center gap-2">
            <div className="position-relative flex-grow-1" style={{ maxWidth: 384 }}>
              <Search size={16} className="position-absolute top-50 translate-middle-y text-body-secondary" style={{ left: 12 }} />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm bài học theo tiêu đề, slug..."
                className={`${inputCls} ps-5`}
              />
            </div>
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className={`${selectCls} w-auto`}
            >
              {categories.map((c) => (
                <option key={c} value={c}>
                  {c === 'ALL' ? 'Tất cả chủ đề' : c}
                </option>
              ))}
            </select>
          </div>
          <span className="fs-xs text-body-secondary">
            Tổng cộng: <strong className="text-body-emphasis">{filtered.length}</strong> bài học
          </span>
        </div>
      </Card>

      <Card className="overflow-hidden">
        {loading ? (
          <Spinner />
        ) : filtered.length === 0 ? (
          <Empty>
            <div className="fw-medium text-body-emphasis">Chưa có bài giảng nào</div>
            <div className="mt-1">Nhấn Tạo bài học để bắt đầu viết nội dung mới.</div>
          </Empty>
        ) : (
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead>
                <tr className="fs-xs text-body-secondary">
                  <th className={`${thCls} ps-3`}>STT</th>
                  <th className={thCls}>Tiêu đề bài học</th>
                  <th className={thCls}>Chủ đề</th>
                  <th className={thCls}>Slug</th>
                  <th className={thCls}>Trạng thái</th>
                  <th className={`${thCls} pe-3 text-end`}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((item, idx) => (
                  <tr key={item.id || idx}>
                    <td className="ps-3 fs-xs text-body-secondary">{item.orderIndex ?? idx + 1}</td>
                    <td className="fw-medium text-body-emphasis">
                      <div className="text-truncate" style={{ maxWidth: 448 }}>{item.title}</div>
                      {item.summary && (
                        <div className="text-truncate fs-xs fw-normal text-body-secondary" style={{ maxWidth: 448 }}>
                          {item.summary}
                        </div>
                      )}
                    </td>
                    <td>
                      <span className="rounded-1 bg-body-secondary px-2 py-1 fs-xs text-body-secondary">
                        {item.category || 'General'}
                      </span>
                    </td>
                    <td className="font-monospace fs-xs text-body-secondary">{item.slug}</td>
                    <td>
                      {item.published ? (
                        <Badge tone="green">
                          <Eye size={12} className="me-1 align-text-bottom" /> Công khai
                        </Badge>
                      ) : (
                        <Badge tone="red">
                          <EyeOff size={12} className="me-1 align-text-bottom" /> Bản nháp
                        </Badge>
                      )}
                    </td>
                    <td className="pe-3 text-end">
                      <div className="d-flex align-items-center justify-content-end gap-1">
                        <button
                          type="button"
                          onClick={() => {
                            setEditItem(item);
                            setEditorOpen(true);
                          }}
                          className={iconBtn}
                          title="Chỉnh sửa"
                        >
                          <Pencil size={16} />
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteId(item.id)}
                          className={`${iconBtn} icon-btn-danger`}
                          title="Xóa bài học"
                        >
                          <Trash2 size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <TheoryEditor
        open={editorOpen}
        theory={editItem}
        onClose={() => setEditorOpen(false)}
        onSaved={() => {
          setEditorOpen(false);
          loadTheories();
        }}
      />

      <ConfirmDialog
        open={Boolean(deleteId)}
        title="Xác nhận xóa bài học lý thuyết"
        message="Hành động này không thể hoàn tác. Học viên sẽ không thể xem nội dung bài giảng này nữa."
        confirmLabel="Xóa bài học"
        busy={deleting}
        onConfirm={confirmDelete}
        onClose={() => setDeleteId(null)}
      />
    </div>
  );
}
