import React from 'react';
import ReactMarkdown from 'react-markdown';

// Element styles live in .markdown-body (src/styles/main.scss)
export default function MarkdownRenderer({ content }) {
  if (!content) return null;

  return (
    <div className="markdown-body">
      <ReactMarkdown>{content}</ReactMarkdown>
    </div>
  );
}
