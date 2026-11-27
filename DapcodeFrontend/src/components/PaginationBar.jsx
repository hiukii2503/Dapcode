import React from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';

function buildPageList(current, total) {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i);
  }
  const pages = [0];
  const start = Math.max(1, Math.min(current - 1, total - 4));
  const end = Math.min(total - 2, Math.max(current + 1, 3));

  if (start > 1) pages.push('ellipsis-left');
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  if (end < total - 2) pages.push('ellipsis-right');
  pages.push(total - 1);
  return pages;
}

export default function PaginationBar({
  page = 0,
  totalPages = 1,
  totalElements = 0,
  pageSize = 20,
  pageSizeOptions = [10, 20, 50],
  itemLabel = 'mục',
  loading = false,
  onPageChange,
  onPageSizeChange,
  className = '',
}) {
  if (!totalElements) return null;

  const safeTotalPages = Math.max(1, totalPages || 1);
  const startItem = page * pageSize + 1;
  const endItem = Math.min((page + 1) * pageSize, totalElements);
  const items = buildPageList(page, safeTotalPages);

  return (
    <div
      className={`d-flex flex-wrap align-items-center justify-content-between gap-2 p-3 ${className}`}
    >
      <div className="fs-xs text-body-secondary">
        Hiển thị <strong className="text-body-emphasis">{startItem}–{endItem}</strong> /{' '}
        <strong className="text-body-emphasis">{totalElements}</strong> {itemLabel}
      </div>

      <nav aria-label="Phân trang" className="d-flex align-items-center gap-1">
        <button
          type="button"
          disabled={loading || page <= 0}
          onClick={() => onPageChange(page - 1)}
          className="icon-btn w-auto px-2 fs-xs"
          title="Trang trước"
        >
          <ChevronLeft size={15} />
          <span className="d-none d-sm-inline">Trước</span>
        </button>

        {items.map((item) =>
          typeof item === 'string' ? (
            <span key={item} className="px-1 fs-xs text-body-secondary user-select-none">
              …
            </span>
          ) : (
            <button
              key={item}
              type="button"
              disabled={loading}
              onClick={() => onPageChange(item)}
              aria-current={item === page ? 'page' : undefined}
              className={
                item === page
                  ? 'btn btn-sm btn-contrast px-2 fs-xs'
                  : 'icon-btn w-auto px-2 fs-xs'
              }
              style={{ minWidth: 32 }}
            >
              {item + 1}
            </button>
          )
        )}

        <button
          type="button"
          disabled={loading || page >= safeTotalPages - 1}
          onClick={() => onPageChange(page + 1)}
          className="icon-btn w-auto px-2 fs-xs"
          title="Trang sau"
        >
          <span className="d-none d-sm-inline">Sau</span>
          <ChevronRight size={15} />
        </button>
      </nav>

      {onPageSizeChange && pageSizeOptions.length > 0 && (
        <div className="d-flex align-items-center gap-2">
          <select
            aria-label="Số mục mỗi trang"
            value={pageSize}
            disabled={loading}
            onChange={(e) => onPageSizeChange(Number(e.target.value))}
            className="form-select form-select-sm w-auto cursor-pointer fs-xs"
          >
            {pageSizeOptions.map((opt) => (
              <option key={opt} value={opt}>
                {opt} / trang
              </option>
            ))}
          </select>
        </div>
      )}
    </div>
  );
}
