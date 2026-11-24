import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Spinner as BsSpinner } from 'react-bootstrap';
import { Plus, Upload, Pencil, Trash2, Eye, EyeOff } from 'lucide-react';
import {
  getTestCases,
  createTestCase,
  createTestCasesBatch,
  updateTestCase,
  deleteTestCase,
  errMsg,
} from '../../api/admin';
import { getProblems } from '../../api/problems';
import {
  Card,
  PageHeader,
  Field,
  Dialog,
  ConfirmDialog,
  Toggle,
  Badge,
  Spinner,
  Empty,
  useToast,
  inputCls,
  selectCls,
  btnPrimary,
  btnGhost,
  iconBtn,
} from '../../components/admin/AdminUi';

const BATCH_SAMPLE = `[
  { "inputData": "[2,7,11,15]\\n9", "expectedOutput": "[0,1]", "hidden": false },
  { "inputData": "[3,2,4]\\n6", "expectedOutput": "[1,2]", "hidden": true }
]`;

function CaseDialog({ open, initial, onClose, onSubmit }) {
  const [form, setForm] = useState({ inputData: '', expectedOutput: '', hidden: false });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) setForm(initial || { inputData: '', expectedOutput: '', hidden: false });
  }, [open, initial]);

  const submit = async () => {
    setSaving(true);
    try {
      await onSubmit(form);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={initial ? 'Chỉnh sửa test case' : 'Thêm test case'}
      footer={
        <>
          <button type="button" className={btnGhost} onClick={onClose}>
            Hủy
          </button>
          <button
            type="button"
            className={btnPrimary}
            disabled={saving || !form.inputData.trim() || !form.expectedOutput.trim()}
            onClick={submit}
          >
            {saving && <BsSpinner animation="border" size="sm" />}
            Lưu
          </button>
        </>
      }
    >
      <div className="d-flex flex-column gap-3">
        <Field label="Dữ liệu đầu vào (input) *">
          <textarea
            className={`${inputCls} font-monospace fs-xs`}
            style={{ height: 112, resize: 'none' }}
            value={form.inputData}
            onChange={(e) => setForm({ ...form, inputData: e.target.value })}
            spellCheck={false}
          />
        </Field>
        <Field label="Kết quả kỳ vọng (expected output) *">
          <textarea
            className={`${inputCls} font-monospace fs-xs`}
            style={{ height: 112, resize: 'none' }}
            value={form.expectedOutput}
            onChange={(e) => setForm({ ...form, expectedOutput: e.target.value })}
            spellCheck={false}
          />
        </Field>
        <Toggle
          checked={form.hidden}
          onChange={(v) => setForm({ ...form, hidden: v })}
          label={form.hidden ? 'Test case ẩn (học viên không thấy)' : 'Test case công khai (hiển thị làm ví dụ)'}
        />
      </div>
    </Dialog>
  );
}

function BatchDialog({ open, onClose, onSubmit }) {
  const [text, setText] = useState('');
  const [saving, setSaving] = useState(false);
  const toast = useToast();

  useEffect(() => {
    if (open) setText('');
  }, [open]);

  const parsed = useMemo(() => {
    if (!text.trim()) return { list: [], error: '' };
    try {
      const v = JSON.parse(text);
      if (!Array.isArray(v) || !v.length) return { list: [], error: 'Cần một mảng JSON có ít nhất 1 phần tử' };
      const list = v.map((t) => ({
        inputData: String(t.inputData ?? t.input ?? ''),
        expectedOutput: String(t.expectedOutput ?? t.output ?? ''),
        hidden: Boolean(t.hidden ?? t.isHidden ?? false),
      }));
      if (list.some((t) => !t.inputData.trim() || !t.expectedOutput.trim())) {
        return { list: [], error: 'Mỗi phần tử cần có inputData và expectedOutput' };
      }
      return { list, error: '' };
    } catch {
      return { list: [], error: 'JSON không hợp lệ' };
    }
  }, [text]);

  const submit = async () => {
    setSaving(true);
    try {
      await onSubmit(parsed.list);
    } catch (e) {
      toast.push('error', errMsg(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      size="lg"
      title="Import test case hàng loạt"
      footer={
        <>
          <button type="button" className={btnGhost} onClick={onClose}>
            Hủy
          </button>
          <button type="button" className={btnPrimary} disabled={saving || !parsed.list.length} onClick={submit}>
            {saving && <BsSpinner animation="border" size="sm" />}
            Import {parsed.list.length ? `${parsed.list.length} test case` : ''}
          </button>
        </>
      }
    >
      <div className="d-flex flex-column gap-3">
        <p className="mb-0 text-body-secondary">
          Dán một mảng JSON gồm các đối tượng <code className="font-monospace">inputData</code>,{' '}
          <code className="font-monospace">expectedOutput</code>, <code className="font-monospace">hidden</code>.
        </p>
        <textarea
          className={`${inputCls} font-monospace fs-xs`}
          style={{ height: 256, resize: 'none' }}
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={BATCH_SAMPLE}
          spellCheck={false}
        />
        {parsed.error && <p className="mb-0 text-danger">{parsed.error}</p>}
        {!parsed.error && parsed.list.length > 0 && (
          <p className="mb-0 text-success">Hợp lệ: {parsed.list.length} test case sẽ được thêm.</p>
        )}
      </div>
    </Dialog>
  );
}

export default function AdminTestCasesPage() {
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const problemId = params.get('problemId') || '';

  const [problems, setProblems] = useState([]);
  const [cases, setCases] = useState([]);
  const [loading, setLoading] = useState(false);
  const [dialog, setDialog] = useState({ open: false, item: null });
  const [batchOpen, setBatchOpen] = useState(false);
  const [toDelete, setToDelete] = useState(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    getProblems({ size: 250 })
      .then((res) => {
        const items = res?.data?.content || res?.content || res?.data || [];
        setProblems(Array.isArray(items) ? items : []);
      })
      .catch((e) => toast.push('error', errMsg(e, 'Không tải được danh sách bài tập')));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const load = useCallback(() => {
    if (!problemId) {
      setCases([]);
      return;
    }
    setLoading(true);
    getTestCases(problemId)
      .then(setCases)
      .catch((e) => toast.push('error', errMsg(e, 'Không tải được test case')))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [problemId]);

  useEffect(() => {
    load();
  }, [load]);

  const current = problems.find((p) => String(p.id) === problemId);
  const hiddenCount = cases.filter((c) => c.hidden).length;

  const handleSave = async (form) => {
    try {
      if (dialog.item) await updateTestCase(dialog.item.id, form);
      else await createTestCase(problemId, form);
      toast.push('success', dialog.item ? 'Đã cập nhật test case' : 'Đã thêm test case');
      setDialog({ open: false, item: null });
      load();
    } catch (e) {
      toast.push('error', errMsg(e));
    }
  };

  const handleBatch = async (list) => {
    await createTestCasesBatch(problemId, list);
    toast.push('success', `Đã import ${list.length} test case`);
    setBatchOpen(false);
    load();
  };

  const confirmDelete = async () => {
    setDeleting(true);
    try {
      await deleteTestCase(toDelete.id);
      toast.push('success', 'Đã xóa test case');
      setToDelete(null);
      load();
    } catch (e) {
      toast.push('error', errMsg(e));
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <PageHeader
        title="Quản lý test case"
        subtitle="Cấu hình bộ test chấm bài cho từng bài tập."
        actions={
          problemId && (
            <>
              <button type="button" className={btnGhost} onClick={() => setBatchOpen(true)}>
                <Upload size={16} />
                Import JSON
              </button>
              <button type="button" className={btnPrimary} onClick={() => setDialog({ open: true, item: null })}>
                <Plus size={16} />
                Thêm test case
              </button>
            </>
          )
        }
      />

      <Card className="mb-3 p-3">
        <div className="d-flex flex-wrap align-items-center gap-3">
          <select
            className={`${selectCls} flex-grow-1`}
            style={{ maxWidth: 448 }}
            value={problemId}
            onChange={(e) => (e.target.value ? setParams({ problemId: e.target.value }) : setParams({}))}
          >
            <option value="">— Chọn bài tập —</option>
            {problems.map((p) => (
              <option key={p.id} value={p.id}>
                {p.title} ({p.difficulty})
              </option>
            ))}
          </select>
          {problemId && !loading && (
            <div className="d-flex align-items-center gap-2 text-body-secondary">
              <Badge>{cases.length} test case</Badge>
              <Badge tone="orange">{hiddenCount} ẩn</Badge>
              <Badge tone="green">{cases.length - hiddenCount} công khai</Badge>
            </div>
          )}
        </div>
      </Card>

      {!problemId ? (
        <Card>
          <Empty>Chọn một bài tập để xem và chỉnh sửa test case.</Empty>
        </Card>
      ) : loading ? (
        <Spinner />
      ) : cases.length === 0 ? (
        <Card>
          <Empty>
            Bài tập "{current?.title}" chưa có test case nào.
          </Empty>
        </Card>
      ) : (
        <div className="d-flex flex-column gap-3">
          {cases.map((c, i) => (
            <Card key={c.id} className="p-3">
              <div className="mb-3 d-flex align-items-center justify-content-between">
                <div className="d-flex align-items-center gap-2">
                  <span className="fw-semibold">Case {i + 1}</span>
                  {c.hidden ? (
                    <Badge tone="orange">
                      <EyeOff size={12} className="me-1 align-text-bottom" />
                      Ẩn
                    </Badge>
                  ) : (
                    <Badge tone="green">
                      <Eye size={12} className="me-1 align-text-bottom" />
                      Công khai
                    </Badge>
                  )}
                </div>
                <div className="d-flex gap-1">
                  <button type="button" className={iconBtn} title="Chỉnh sửa" onClick={() => setDialog({ open: true, item: c })}>
                    <Pencil size={16} />
                  </button>
                  <button type="button" className={`${iconBtn} icon-btn-danger`} title="Xóa" onClick={() => setToDelete(c)}>
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
              <div className="row g-3">
                <div className="col-12 col-md-6">
                  <div className="mb-1 fs-xs text-body-secondary">Input</div>
                  <pre className="mb-0 overflow-auto rounded-2 bg-body-tertiary p-3 font-monospace fs-xs" style={{ maxHeight: 160 }}>
                    {c.inputData}
                  </pre>
                </div>
                <div className="col-12 col-md-6">
                  <div className="mb-1 fs-xs text-body-secondary">Expected output</div>
                  <pre className="mb-0 overflow-auto rounded-2 bg-body-tertiary p-3 font-monospace fs-xs" style={{ maxHeight: 160 }}>
                    {c.expectedOutput}
                  </pre>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <CaseDialog
        open={dialog.open}
        initial={dialog.item}
        onClose={() => setDialog({ open: false, item: null })}
        onSubmit={handleSave}
      />
      <BatchDialog open={batchOpen} onClose={() => setBatchOpen(false)} onSubmit={handleBatch} />
      <ConfirmDialog
        open={Boolean(toDelete)}
        onClose={() => setToDelete(null)}
        onConfirm={confirmDelete}
        busy={deleting}
        title="Xóa test case"
        confirmLabel="Xóa"
        message="Test case này sẽ bị xóa vĩnh viễn khỏi bộ test chấm bài."
      />
    </>
  );
}
