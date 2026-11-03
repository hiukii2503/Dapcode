import React from 'react';

export default function DifficultyBadge({ difficulty = 'Easy', size = 'sm', variant = 'pill' }) {
  const diff = String(difficulty).toLowerCase();

  const config = {
    easy: { label: 'Easy', text: 'text-easy', pill: 'bg-easy-soft' },
    medium: { label: 'Medium', text: 'text-medium', pill: 'bg-medium-soft' },
    hard: { label: 'Hard', text: 'text-hard', pill: 'bg-hard-soft' },
  }[diff] || {
    label: difficulty,
    text: 'text-body-secondary',
    pill: 'bg-secondary bg-opacity-10',
  };

  if (variant === 'text') {
    return <span className={config.text}>{config.label}</span>;
  }

  const sizeClasses = size === 'xs' ? 'px-2' : 'px-3';

  return <span className={`badge rounded-pill ${sizeClasses} ${config.text} ${config.pill}`}>{config.label}</span>;
}
