import React from 'react';
import Editor from '@monaco-editor/react';
import { RotateCcw, Sparkles, Code2 } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { FEATURES } from '../config/features';

export const SUPPORTED_LANGUAGES = [
  { id: 71, key: 'python', label: 'Python 3', monacoLang: 'python' },
  { id: 62, key: 'java', label: 'Java (OpenJDK 13)', monacoLang: 'java' },
  { id: 54, key: 'cpp', label: 'C++ (GCC 9.2)', monacoLang: 'cpp' },
];

export default function CodeEditor({
  code,
  onChange,
  language,
  onLanguageChange,
  onResetCode,
  onOpenAiTutor,
}) {
  const { isDark } = useTheme();
  const currentLangObj = SUPPORTED_LANGUAGES.find((l) => l.key === language) || SUPPORTED_LANGUAGES[0];

  const handleEditorWillMount = (monaco) => {
    monaco.editor.defineTheme('dapcode-light', {
      base: 'vs',
      inherit: true,
      rules: [
        { token: 'comment', foreground: '8A8A8A', fontStyle: 'italic' },
        { token: 'keyword', foreground: '0000FF' },
        { token: 'string', foreground: 'A31515' },
        { token: 'number', foreground: '098658' },
        { token: 'type', foreground: '267F99' },
      ],
      colors: {
        'editor.background': '#FFFFFF',
        'editor.foreground': '#262626',
        'editorLineNumber.foreground': '#9A9A9A',
        'editorLineNumber.activeForeground': '#595959',
        'editor.lineHighlightBackground': '#F7F8FA',
        'editorCursor.foreground': '#262626',
        'editor.selectionBackground': '#D9E8FF',
        'editorIndentGuide.background': '#EFEFEF',
        'editorIndentGuide.activeBackground': '#D1D1D1',
      },
    });

    monaco.editor.defineTheme('dapcode-dark', {
      base: 'vs-dark',
      inherit: true,
      rules: [
        { token: 'comment', foreground: '7A7A7A', fontStyle: 'italic' },
        { token: 'keyword', foreground: '569CD6' },
        { token: 'string', foreground: 'CE9178' },
        { token: 'number', foreground: 'B5CEA8' },
        { token: 'type', foreground: '4EC9B0' },
      ],
      colors: {
        'editor.background': '#282828',
        'editor.foreground': '#E6E6E6',
        'editorLineNumber.foreground': '#6B6B6B',
        'editorLineNumber.activeForeground': '#9A9A9A',
        'editor.lineHighlightBackground': '#303030',
        'editorCursor.foreground': '#EFF1F6',
        'editor.selectionBackground': '#3E4B63',
        'editorIndentGuide.background': '#343434',
        'editorIndentGuide.activeBackground': '#595959',
      },
    });
  };

  return (
    <div className="d-flex h-100 flex-column bg-surface rounded-2 overflow-hidden">
      {/* Editor Toolbar */}
      <div
        className="d-flex align-items-center justify-content-between border-bottom bg-body-tertiary px-3 flex-shrink-0"
        style={{ height: 40 }}
      >
        {/* Language selector */}
        <div className="d-flex align-items-center gap-3">
          <div className="d-flex align-items-center gap-2 fs-xs text-body-secondary">
            <Code2 size={14} />
            <span className="fw-medium d-none d-sm-inline fs-2xs">Ngôn ngữ:</span>
          </div>
          <select
            value={language}
            onChange={(e) => onLanguageChange(e.target.value)}
            className="form-select form-select-sm w-auto fs-xs fw-medium bg-surface border"
          >
            {SUPPORTED_LANGUAGES.map((lang) => (
              <option key={lang.key} value={lang.key}>
                {lang.label}
              </option>
            ))}
          </select>
        </div>

        {/* Right Tools: AI Tutor & Reset */}
        <div className="d-flex align-items-center gap-2">
          {/* Socratic AI Tutor Button (wrapper keeps the tooltip on a disabled .btn) */}
          <span className="d-inline-block" title={FEATURES.aiTutor ? undefined : 'Tính năng đang phát triển'}>
            <button
              type="button"
              onClick={onOpenAiTutor}
              disabled={!FEATURES.aiTutor}
              className="btn btn-sm btn-contrast d-inline-flex align-items-center gap-1 fs-xs"
            >
              <Sparkles size={14} />
              <span>AI Tutor</span>
            </button>
          </span>

          {/* Reset Code */}
          <button
            type="button"
            onClick={onResetCode}
            title="Khôi phục mã mẫu ban đầu"
            className="btn btn-sm btn-ghost border d-inline-flex align-items-center gap-1 fs-xs"
          >
            <RotateCcw size={14} />
            <span className="d-none d-sm-inline">Reset</span>
          </button>
        </div>
      </div>

      {/* Monaco Editor Container */}
      <div className="flex-grow-1 w-100 position-relative min-h-0">
        <Editor
          height="100%"
          language={currentLangObj.monacoLang}
          value={code}
          theme={isDark ? 'dapcode-dark' : 'dapcode-light'}
          beforeMount={handleEditorWillMount}
          onChange={(val) => onChange(val || '')}
          options={{
            fontSize: 13,
            fontFamily: "'Fira Code', 'JetBrains Mono', Consolas, monospace",
            lineHeight: 22,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            automaticLayout: true,
            tabSize: 4,
            bracketPairColorization: { enabled: true },
            wordWrap: 'on',
            padding: { top: 12, bottom: 12 },
          }}
        />
      </div>
    </div>
  );
}
