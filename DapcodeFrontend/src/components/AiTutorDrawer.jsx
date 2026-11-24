import React, { useState } from 'react';
import { Offcanvas, Spinner } from 'react-bootstrap';
import {
  Sparkles,
  Lightbulb,
  FileCode2,
  AlertCircle,
  Bookmark,
  Bot
} from 'lucide-react';
import MarkdownRenderer from './MarkdownRenderer';
import { getAiHint } from '../api/ai';

export default function AiTutorDrawer({
  isOpen,
  onClose,
  problemSlug,
  problemTitle,
  currentCode,
  language,
  lastError,
}) {
  const [hintLevel, setHintLevel] = useState(1); // 1: Nudge, 2: Pseudocode
  const [question, setQuestion] = useState('');
  const [includeCode, setIncludeCode] = useState(true);
  const [isLoading, setIsLoading] = useState(false);
  const [hintResponse, setHintResponse] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  const handleAskHint = async () => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const res = await getAiHint({
        problemSlug,
        code: includeCode ? currentCode : '',
        language,
        hintLevel,
        errorMessage: lastError || '',
        question,
      });

      if (res && res.data) {
        setHintResponse(res.data);
      } else if (res) {
        setHintResponse(res);
      }
    } catch (err) {
      console.error('Failed to call AI Tutor API:', err);
      if (err.response?.status === 401) {
        setErrorMsg('Vui lòng đăng nhập tài khoản để sử dụng trợ lý AI Tutor!');
      } else {
        const message = err.response?.data?.message || err.message || 'Không thể kết nối tới AI Tutor. Vui lòng thử lại sau.';
        setErrorMsg(message);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const levelCls = (level) =>
    `btn w-100 h-100 d-flex flex-column align-items-start text-start p-3 rounded-3 border ${
      hintLevel === level ? 'border-primary bg-body-tertiary' : 'hover-bg'
    }`;

  return (
    <Offcanvas show={isOpen} onHide={onClose} placement="end" className="offcanvas-lg-width">
      {/* Drawer Header */}
      <Offcanvas.Header closeButton className="border-bottom bg-body-tertiary px-4">
        <div className="d-flex align-items-center gap-2 min-w-0">
          <div
            className="d-flex align-items-center justify-content-center rounded-2 bg-primary text-white flex-shrink-0"
            style={{ width: 28, height: 28 }}
          >
            <Sparkles size={16} />
          </div>
          <div className="min-w-0">
            <Offcanvas.Title as="h2" className="fs-sm fw-semibold d-flex align-items-center gap-2 mb-0">
              <span>Socratic AI Tutor</span>
              <span className="badge rounded-1 bg-primary bg-opacity-25 text-primary border border-primary-subtle fs-2xs px-2 py-0">
                LiệtCode
              </span>
            </Offcanvas.Title>
            <div className="fs-2xs text-body-secondary text-truncate" style={{ maxWidth: 260 }}>
              {problemTitle}
            </div>
          </div>
        </div>
      </Offcanvas.Header>

      {/* Drawer Scrollable Body */}
      <Offcanvas.Body className="d-flex flex-column gap-4 p-4">
        {/* Hint Level Selector */}
        <div>
          <div className="form-label mb-2">
            Chọn mức độ hỗ trợ (Socratic Level):
          </div>
          <div className="row g-2">
            <div className="col-6">
              <button type="button" onClick={() => setHintLevel(1)} className={levelCls(1)}>
                <div className="d-flex align-items-center gap-2 fs-xs text-body-emphasis mb-1">
                  <Lightbulb size={14} />
                  <span className={hintLevel === 1 ? 'fw-bold' : 'fw-medium'}>Level 1: Gợi ý tư duy</span>
                </div>
                <p className="fs-2xs fw-normal text-body-secondary lh-sm mb-0">
                  Định hướng suy nghĩ, không tiết lộ mã giả hay thuật toán trực tiếp.
                </p>
              </button>
            </div>

            <div className="col-6">
              <button type="button" onClick={() => setHintLevel(2)} className={levelCls(2)}>
                <div className="d-flex align-items-center gap-2 fs-xs text-body-emphasis mb-1">
                  <FileCode2 size={14} />
                  <span className={hintLevel === 2 ? 'fw-bold' : 'fw-medium'}>Level 2: Mã giả (Pseudocode)</span>
                </div>
                <p className="fs-2xs fw-normal text-body-secondary lh-sm mb-0">
                  Cung cấp các bước logic bằng mã giả để tự tay code lại.
                </p>
              </button>
            </div>
          </div>
        </div>

        {/* Question / Context Input */}
        <div>
          <label htmlFor="ai-tutor-question" className="form-label d-block">
            Câu hỏi hoặc điểm vướng mắc của bạn:
          </label>
          <textarea
            id="ai-tutor-question"
            rows={3}
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="VD: Thuật toán của tôi đang bị Time Limit Exceeded, có cách nào tối ưu thời gian từ O(N^2) xuống O(N) không?"
            className="form-control fs-xs"
          />
        </div>

        {/* Include current code switch */}
        <div className="d-flex align-items-center justify-content-between gap-3 rounded-2 border bg-body-tertiary px-3 py-2">
          <span className="fs-2xs">Tự động gửi code hiện tại để AI phân tích</span>
          <input
            type="checkbox"
            checked={includeCode}
            onChange={(e) => setIncludeCode(e.target.checked)}
            className="form-check-input mt-0 flex-shrink-0 cursor-pointer"
          />
        </div>

        {/* Submit Ask Button */}
        <button
          type="button"
          disabled={isLoading}
          onClick={handleAskHint}
          className="btn btn-primary w-100 d-flex align-items-center justify-content-center gap-2 fs-xs py-2"
        >
          {isLoading ? (
            <>
              <Spinner animation="border" size="sm" />
              <span>AI Tutor đang suy luận...</span>
            </>
          ) : (
            <>
              <Sparkles size={16} />
              <span>Xin gợi ý từ AI Tutor</span>
            </>
          )}
        </button>

        {/* Error Message */}
        {errorMsg && (
          <div className="d-flex align-items-center gap-2 rounded-2 border border-danger-subtle bg-danger-subtle text-danger-emphasis p-3 fs-xs">
            <AlertCircle size={16} className="flex-shrink-0" />
            <span>{errorMsg}</span>
          </div>
        )}

        {/* AI Response Display: annotation style with a left accent border */}
        {hintResponse && (
          <div className="d-flex flex-column gap-3 border-start border-2 border-primary rounded-end-3 bg-body-tertiary p-3">
            <div className="d-flex align-items-center justify-content-between gap-2 border-bottom pb-2">
              <div className="d-flex align-items-center gap-2 fs-xs fw-semibold text-body-emphasis">
                <Bot size={16} />
                <span>Phản hồi từ AI Tutor (Level {hintResponse.hint_lv || hintLevel})</span>
              </div>
              {hintResponse.related_concept && (
                <span className="d-inline-flex align-items-center gap-1 rounded-1 border bg-surface px-2 py-1 fs-2xs fw-medium">
                  <Bookmark size={12} />
                  <span>{hintResponse.related_concept}</span>
                </span>
              )}
            </div>

            {/* Rendered Markdown Body */}
            <MarkdownRenderer content={hintResponse.hint} />
          </div>
        )}
      </Offcanvas.Body>
    </Offcanvas>
  );
}
