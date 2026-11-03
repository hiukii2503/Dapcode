import React, { useState } from 'react';
import { Spinner } from 'react-bootstrap';
import {
  Play,
  Send,
  Terminal,
  CheckCircle2,
  XCircle,
  Clock,
  Cpu,
  AlertCircle
} from 'lucide-react';
import { FEATURES } from '../config/features';

const preWrap = { whiteSpace: 'pre-wrap' };

export default function ConsoleDrawer({
  publicTestCases = [],
  submissionState, // { isRunning, isSubmitting, progress, result, error }
  onRunCode,
  onSubmitCode,
}) {
  const [activeTab, setActiveTab] = useState('testcases'); // 'testcases' | 'result'
  const [selectedTestCaseIndex, setSelectedTestCaseIndex] = useState(0);

  const { isRunning, isSubmitting, progress, result, error } = submissionState;
  const isBusy = isRunning || isSubmitting;
  const judgeTitle = FEATURES.judge ? undefined : 'Tính năng đang phát triển';

  const currentCase = publicTestCases[selectedTestCaseIndex] || publicTestCases[0] || {
    inputData: '',
    expectedOutput: '',
  };

  const getVerdictStyle = (status) => {
    switch (status?.toUpperCase()) {
      case 'ACCEPTED':
        return {
          label: 'Accepted',
          badgeClass: 'bg-success-subtle text-success-emphasis border-success-subtle',
          icon: CheckCircle2,
          iconClass: 'text-success',
        };
      case 'WRONG_ANSWER':
        return {
          label: 'Wrong Answer',
          badgeClass: 'bg-danger-subtle text-danger-emphasis border-danger-subtle',
          icon: XCircle,
          iconClass: 'text-danger',
        };
      case 'TIME_LIMIT_EXCEEDED':
        return {
          label: 'Time Limit Exceeded',
          badgeClass: 'bg-primary-subtle text-primary-emphasis border-primary-subtle',
          icon: Clock,
          iconClass: 'text-primary',
        };
      default:
        return {
          label: status || 'Error',
          badgeClass: 'bg-danger-subtle text-danger-emphasis border-danger-subtle',
          icon: AlertCircle,
          iconClass: 'text-danger',
        };
    }
  };

  return (
    <div className="d-flex h-100 flex-column bg-surface rounded-2 overflow-hidden">
      {/* Drawer Header Tabs */}
      <div
        className="d-flex align-items-center justify-content-between border-bottom bg-body-tertiary px-3 flex-shrink-0"
        style={{ height: 40 }}
      >
        <nav className="nav nav-pills nav-soft">
          <button
            type="button"
            onClick={() => setActiveTab('testcases')}
            className={`nav-link fs-xs ${activeTab === 'testcases' ? 'active' : ''}`}
          >
            <Terminal size={14} />
            <span>Test Cases</span>
          </button>

          <button
            type="button"
            onClick={() => setActiveTab('result')}
            className={`nav-link fs-xs ${activeTab === 'result' ? 'active' : ''}`}
          >
            <span>Kết quả</span>
            {isBusy && <Spinner animation="grow" className="ms-1" style={{ width: 6, height: 6 }} />}
          </button>
        </nav>

        {/* Action Buttons: Run & Submit (wrappers keep the tooltip on a disabled .btn) */}
        <div className="d-flex align-items-center gap-2">
          <span className="d-inline-block" title={judgeTitle}>
            <button
              type="button"
              disabled={isBusy || !FEATURES.judge}
              onClick={() => {
                setActiveTab('result');
                onRunCode();
              }}
              className="btn btn-sm btn-ghost border d-inline-flex align-items-center gap-1 fs-xs"
            >
              {isRunning ? (
                <Spinner animation="border" size="sm" />
              ) : (
                <Play size={14} className="text-body-secondary" fill="currentColor" />
              )}
              <span>Run Code</span>
            </button>
          </span>

          <span className="d-inline-block" title={judgeTitle}>
            <button
              type="button"
              disabled={isBusy || !FEATURES.judge}
              onClick={() => {
                setActiveTab('result');
                onSubmitCode();
              }}
              className="btn btn-sm btn-success d-inline-flex align-items-center gap-1 fs-xs"
            >
              {isSubmitting ? <Spinner animation="border" size="sm" /> : <Send size={14} />}
              <span>Submit</span>
            </button>
          </span>
        </div>
      </div>

      {/* Drawer Body Content */}
      <div className="flex-grow-1 overflow-y-auto p-3 fs-xs">
        {activeTab === 'testcases' ? (
          <div>
            {/* Case selector pills */}
            <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
              {publicTestCases.map((tc, idx) => (
                <button
                  key={tc.id || idx}
                  type="button"
                  onClick={() => setSelectedTestCaseIndex(idx)}
                  className={`btn btn-sm font-monospace fs-xs ${
                    selectedTestCaseIndex === idx ? 'btn-contrast' : 'btn-ghost border fw-normal'
                  }`}
                >
                  Case {idx + 1}
                </button>
              ))}
            </div>

            {/* Selected case details */}
            <div className="d-flex flex-column gap-3">
              <div>
                <div className="fs-2xs fw-medium text-body-secondary mb-1">Input:</div>
                <div className="font-monospace rounded-2 border bg-body-tertiary p-2">
                  {currentCase.inputData || 'N/A'}
                </div>
              </div>

              <div>
                <div className="fs-2xs fw-medium text-body-secondary mb-1">Expected Output:</div>
                <div className="font-monospace rounded-2 border bg-body-tertiary p-2">
                  {currentCase.expectedOutput !== undefined && currentCase.expectedOutput !== null
                    ? (currentCase.expectedOutput === '' ? '"" (chuỗi rỗng)' : currentCase.expectedOutput)
                    : 'N/A'}
                </div>
              </div>
            </div>
          </div>
        ) : (
          /* Test Result / Submission SSE Stream View */
          <div>
            {isBusy ? (
              <div className="d-flex flex-column align-items-center justify-content-center py-5 gap-3">
                <Spinner animation="border" size="sm" />
                <div className="fs-xs fw-medium">
                  {progress?.message || 'Đang gửi mã nguồn tới Sandbox Judge0...'}
                </div>
                {progress?.currentTest !== undefined && progress?.totalTests && (
                  <div className="d-flex flex-column gap-1" style={{ width: 224 }}>
                    <div className="d-flex justify-content-between fs-2xs text-body-secondary">
                      <span>Kiểm thử test case {progress.currentTest}/{progress.totalTests}</span>
                      <span>{Math.round((progress.currentTest / progress.totalTests) * 100)}%</span>
                    </div>
                    <div className="progress">
                      <div
                        className="progress-bar"
                        style={{ width: `${(progress.currentTest / progress.totalTests) * 100}%` }}
                      />
                    </div>
                  </div>
                )}
              </div>
            ) : error ? (
              <div className="d-flex flex-column gap-2 rounded-2 border border-danger-subtle bg-danger-subtle text-danger-emphasis p-3">
                <div className="d-flex align-items-center gap-2 fw-medium">
                  <AlertCircle size={16} className="text-danger" />
                  <span>Đã xảy ra lỗi</span>
                </div>
                <div className="font-monospace fs-xs bg-surface p-2 rounded-1 border border-danger-subtle" style={preWrap}>
                  {error}
                </div>
              </div>
            ) : result ? (
              <div className="d-flex flex-column gap-3">
                {/* Verdict Banner */}
                {(() => {
                  const verdict = getVerdictStyle(result.status);
                  const Icon = verdict.icon;
                  return (
                    <div className="d-flex align-items-center justify-content-between border-bottom pb-3">
                      <span className={`d-inline-flex align-items-center gap-2 rounded-2 border px-3 py-1 fs-xs fw-medium ${verdict.badgeClass}`}>
                        <Icon size={16} className={verdict.iconClass} />
                        <span>{verdict.label}</span>
                      </span>

                      {/* Runtime & Memory */}
                      <div className="d-flex align-items-center gap-3 fs-xs text-body-secondary">
                        {result.totalRuntimeMs !== undefined && (
                          <div className="d-flex align-items-center gap-1">
                            <Clock size={14} />
                            <span>Runtime: <strong className="text-body font-monospace">{result.totalRuntimeMs} ms</strong></span>
                          </div>
                        )}
                        {result.maxMemoryKb !== undefined && (
                          <div className="d-flex align-items-center gap-1">
                            <Cpu size={14} />
                            <span>Memory: <strong className="text-body font-monospace">{(result.maxMemoryKb / 1024).toFixed(1)} MB</strong></span>
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })()}

                {(() => {
                  const casesToDisplay = (result.testCaseResults && result.testCaseResults.length > 0)
                    ? result.testCaseResults
                    : (publicTestCases && publicTestCases.length > 0)
                      ? publicTestCases.map((ptc, idx) => ({
                          testCaseId: ptc.id || idx,
                          status: result.status === 'ACCEPTED' ? 'ACCEPTED' : (result.status || 'PENDING'),
                          inputData: ptc.inputData,
                          expectedOutput: ptc.expectedOutput,
                          actualOutput: result.status === 'ACCEPTED' ? ptc.expectedOutput : 'N/A',
                          isHidden: false,
                        }))
                      : [];

                  if (casesToDisplay.length === 0) {
                    return (
                      <div className="rounded-3 border bg-body-tertiary p-3 text-center fs-xs text-body-secondary">
                        Đã kiểm thử hoàn tất với kết quả: <strong className="font-monospace">{result.status || 'ACCEPTED'}</strong>
                      </div>
                    );
                  }

                  return (
                    <div className="d-flex flex-column gap-3">
                      <div className="fs-2xs fw-medium">
                        Chi tiết từng Test Case ({result.passedTestCases ?? casesToDisplay.filter(c => c.status === 'ACCEPTED' || c.passed).length} / {casesToDisplay.length} Passed):
                      </div>

                      <div className="d-flex flex-column gap-2">
                        {casesToDisplay.map((tc, i) => {
                          const isPassed = tc.status === 'ACCEPTED' || tc.passed === true;
                          const isHidden = tc.isHidden ?? tc.hidden ?? false;
                          let cleanActual = 'N/A';
                          if (tc.actualOutput !== undefined && tc.actualOutput !== null) {
                            const trimmed = tc.actualOutput.trim();
                            cleanActual = trimmed === '' ? '"" (chuỗi rỗng)' : trimmed;
                          } else if (isPassed) {
                            cleanActual = (tc.expectedOutput !== undefined && tc.expectedOutput !== null && tc.expectedOutput.trim() === '')
                              ? '"" (chuỗi rỗng)'
                              : (tc.expectedOutput || 'Passed');
                          }
                          if (cleanActual && cleanActual.length >= 16 && /^[A-Za-z0-9+/=\s]+$/.test(cleanActual) && !cleanActual.startsWith('[') && !cleanActual.startsWith('{')) {
                            try {
                              const decoded = atob(cleanActual.replace(/\s+/g, ''));
                              if (decoded && /^[\x20-\x7E\r\n\t]+$/.test(decoded)) {
                                cleanActual = decoded;
                              }
                            } catch {}
                          }

                          return (
                            <div
                              key={i}
                              className={`rounded-2 border p-3 fs-xs ${
                                isPassed ? 'bg-body-tertiary' : 'bg-danger-subtle border-danger-subtle'
                              }`}
                            >
                              <div className="d-flex align-items-center justify-content-between pb-2 mb-2 border-bottom">
                                <div className="d-flex align-items-center gap-2">
                                  <span className="font-monospace fw-semibold">
                                    Case {i + 1} {isHidden ? '(Ẩn)' : ''}
                                  </span>
                                  <span
                                    className={`badge rounded-1 border text-uppercase fw-semibold fs-2xs ${
                                      isPassed
                                        ? 'bg-success-subtle text-success-emphasis border-success-subtle'
                                        : 'bg-danger-subtle text-danger-emphasis border-danger-subtle'
                                    }`}
                                  >
                                    {isPassed ? 'PASS' : (tc.status || 'FAIL')}
                                  </span>
                                </div>
                                {tc.runtimeMs !== undefined && (
                                  <span className="fs-2xs text-body-secondary">
                                    {Math.round(tc.runtimeMs)} ms
                                  </span>
                                )}
                              </div>

                              <div className="row g-2">
                                <div className="col-12 col-md-4">
                                  <div className="fs-2xs fw-medium text-body-secondary mb-1">
                                    Input:
                                  </div>
                                  <div className="font-monospace rounded-2 border bg-surface p-2 text-break" style={preWrap}>
                                    {tc.inputData || (isHidden ? '(Test case ẩn)' : 'N/A')}
                                  </div>
                                </div>

                                <div className="col-12 col-md-4">
                                  <div className="fs-2xs fw-medium text-body-secondary mb-1">
                                    Output:
                                  </div>
                                  <div className="font-monospace rounded-2 border bg-surface p-2 text-break" style={preWrap}>
                                    {tc.expectedOutput !== undefined && tc.expectedOutput !== null
                                      ? (tc.expectedOutput === '' ? '"" (chuỗi rỗng)' : tc.expectedOutput)
                                      : (isHidden ? '(Test case ẩn)' : 'N/A')}
                                  </div>
                                </div>

                                <div className="col-12 col-md-4">
                                  <div className="fs-2xs fw-medium text-body-secondary mb-1">
                                    Kết quả:
                                  </div>
                                  <div
                                    className={`font-monospace rounded-2 border p-2 text-break fw-medium ${
                                      isPassed
                                        ? 'bg-success-subtle text-success-emphasis border-success-subtle'
                                        : 'bg-danger-subtle text-danger-emphasis border-danger-subtle'
                                    }`}
                                    style={preWrap}
                                  >
                                    {cleanActual}
                                  </div>
                                </div>
                              </div>

                              {tc.errMes && (
                                <div className="mt-2 fs-2xs font-monospace bg-danger-subtle text-danger-emphasis p-2 rounded-1 border border-danger-subtle" style={preWrap}>
                                  {tc.errMes}
                                </div>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  );
                })()}
              </div>
            ) : (
              <div className="d-flex flex-column align-items-center justify-content-center py-5 text-center text-body-secondary">
                <Terminal size={24} className="mb-2 opacity-50" />
                <p className="fs-xs mb-0">Nhấn &quot;Run Code&quot; hoặc &quot;Submit&quot; để xem kết quả kiểm thử.</p>
                {!FEATURES.judge && (
                  <p className="fs-2xs mt-1 mb-0">Chấm bài chưa được hỗ trợ trên backend demo.</p>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
