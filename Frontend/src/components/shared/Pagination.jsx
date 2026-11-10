import { ChevronLeft, ChevronRight } from "lucide-react";

/**
 * Controles de paginación reutilizables.
 * Props:
 *   pagina       – índice 0-based de la página actual
 *   totalItems   – total de elementos a paginar
 *   pageSize     – elementos por página
 *   onChange(n)  – callback al cambiar de página
 */
export default function Pagination({ pagina, totalItems, pageSize, onChange }) {
  const total = Math.max(1, Math.ceil(totalItems / pageSize));
  const actual = Math.min(pagina, total - 1);

  return (
    <div className="flex items-center gap-1">
      <button
        className="btn btn-secondary px-3 disabled:opacity-30"
        disabled={actual === 0}
        onClick={() => onChange(actual - 1)}
      >
        <ChevronLeft size={16} />
      </button>
      <span className="text-[13px] font-semibold text-gray-700 min-w-[52px] text-center tabular-nums">
        {actual + 1} / {total}
      </span>
      <button
        className="btn btn-secondary px-3 disabled:opacity-30"
        disabled={actual >= total - 1}
        onClick={() => onChange(actual + 1)}
      >
        <ChevronRight size={16} />
      </button>
    </div>
  );
}
