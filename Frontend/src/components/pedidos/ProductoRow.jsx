import { formatCOP } from "../../utils/format";

/** Fila de producto en el catálogo con controles de cantidad +/−. */
export default function ProductoRow({ producto, cantidad, onChange }) {
  const activo = cantidad > 0;
  return (
    <div className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors ${activo ? "bg-verde-bg" : "hover:bg-gray-50"}`}>
      <div className="flex-1 min-w-0">
        <div className={`text-sm font-medium truncate ${activo ? "text-verde-oscuro font-semibold" : "text-gray-900"}`}>
          {producto.nombre}
        </div>
        <div className="text-[11px] text-gray-400">{formatCOP(producto.precio)}</div>
      </div>

      {cantidad === 0 ? (
        <button
          onClick={() => onChange(1)}
          className="w-7 h-7 rounded-full bg-verde-claro text-white flex items-center justify-center text-lg leading-none font-bold hover:bg-verde-oscuro transition-colors shrink-0"
        >
          +
        </button>
      ) : (
        <div className="flex items-center gap-0.5 bg-white border border-verde-claro rounded-full px-1 shrink-0">
          <button
            onClick={() => onChange(cantidad - 1)}
            className="w-6 h-6 flex items-center justify-center text-verde-oscuro font-bold text-lg leading-none hover:bg-gray-100 rounded-full transition-colors"
          >
            −
          </button>
          <input
            type="number"
            min="0"
            className="w-10 text-center text-sm font-bold text-verde-oscuro bg-transparent border-0 focus:outline-none focus:ring-1 focus:ring-verde-claro rounded"
            value={cantidad}
            onChange={(e) => {
              const n = e.target.value === "" ? 0 : Number(e.target.value);
              if (!isNaN(n) && n >= 0) onChange(n);
            }}
            onClick={(e) => e.target.select()}
          />
          <button
            onClick={() => onChange(cantidad + 1)}
            className="w-6 h-6 flex items-center justify-center text-verde-oscuro font-bold text-lg leading-none hover:bg-gray-100 rounded-full transition-colors"
          >
            +
          </button>
        </div>
      )}
    </div>
  );
}
