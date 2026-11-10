import { useState } from "react";
import { Plus, Search, Utensils, X } from "lucide-react";
import { formatCOP } from "../../utils/format";
import { normalizarFracciones } from "../../utils/pedidoHelpers";
import ProductoRow from "./ProductoRow";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/**
 * Modal para agregar ítems a un pedido existente.
 * Calcula el diff vs el estado actual y llama onConfirmar({ diffs, manuales }).
 */
export default function ModalAgregarItems({ productos, detalles = [], onConfirmar, onCancelar }) {
  const [cantidades, setCantidades]     = useState({});
  const [busqueda, setBusqueda]         = useState("");
  const [manual, setManual]             = useState({ nombre: "", precio: "", cantidad: 1 });
  const [manualItems, setManualItems]   = useState([]);
  const [error, setError]               = useState("");

  const cambiarCantidad = (id, n) =>
    setCantidades((c) => {
      const updated = n <= 0
        ? (() => { const { [id]: _, ...rest } = c; return rest; })()
        : { ...c, [id]: n };
      return normalizarFracciones(updated, productos);
    });

  const agregarManual = () => {
    if (!manual.nombre.trim()) return;
    const cant = Math.max(1, parseInt(manual.cantidad, 10) || 1);
    setManualItems((ms) => [...ms, { ...manual, cantidad: cant, _key: Date.now() }]);
    setManual({ nombre: "", precio: "", cantidad: 1 });
  };

  const productosFiltrados = productos.filter((p) =>
    !busqueda || p.nombre.toLowerCase().includes(busqueda.toLowerCase())
  );

  const seleccionados = productos
    .filter((p) => (cantidades[p.id] ?? 0) > 0)
    .map((p) => ({ ...p, cantidad: cantidades[p.id] }));

  const totalItems = seleccionados.reduce((s, p) => s + p.cantidad, 0)
    + manualItems.reduce((s, m) => s + m.cantidad, 0);

  const handleConfirmar = () => {
    if (totalItems === 0) { setError("Selecciona al menos un ítem"); return; }

    const manuales = manualItems.map((m) => ({
      nombreManual: m.nombre.trim(),
      precioManual: parseFloat(m.precio) || 0,
      cantidad: m.cantidad,
    }));

    const existingMap = Object.fromEntries(
      detalles.filter((d) => d.productoId).map((d) => [d.productoId, d.cantidad])
    );
    const combined = { ...existingMap };
    for (const [k, v] of Object.entries(cantidades)) {
      combined[Number(k)] = (combined[Number(k)] ?? 0) + v;
    }
    const normalized = normalizarFracciones(combined, productos);

    const allKeys = new Set([
      ...Object.keys(existingMap).map(Number),
      ...Object.keys(normalized).map(Number),
    ]);
    const diffs = [];
    for (const pid of allKeys) {
      const before = existingMap[pid] ?? 0;
      const after  = normalized[pid]  ?? 0;
      if (before !== after) diffs.push({ productoId: pid, cantidadAntes: before, cantidadDespues: after });
    }

    onConfirmar({ diffs, manuales });
  };

  // Esc cancela; Enter confirma (excepto cuando el foco está dentro del form de ítems manuales)
  useModalKeyboard({ onEscape: onCancelar, onEnter: handleConfirmar });

  return (
    /* z-[1050] — encima de ModalDetalle (1000), debajo de ConfirmDialog (1100) */
    <div className="fixed inset-0 bg-black/50 z-[1050] flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl flex flex-col overflow-hidden animate-slide-up" style={{ height: "85vh" }}>

        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
          <div>
            <h2 className="text-verde-claro font-extrabold text-xl">Agregar ítems al pedido</h2>
            <p className="text-[12px] text-gray-400 mt-0.5">Selecciona los productos a agregar y confirma al finalizar</p>
          </div>
          <button className="btn-icon" onClick={onCancelar}><X size={20} /></button>
        </div>

        {/* Body: dos columnas */}
        <div className="flex flex-1 overflow-hidden">

          {/* Izquierda: catálogo */}
          <div className="flex flex-col border-r border-gray-200 overflow-hidden" style={{ width: "58%" }}>
            <div className="px-4 py-2 border-b border-gray-100 shrink-0">
              <div className="relative">
                <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
                <input
                  className="form-control pl-8 pr-8 text-sm"
                  placeholder="Buscar producto…"
                  value={busqueda}
                  onChange={(e) => setBusqueda(e.target.value)}
                />
                {busqueda && (
                  <button
                    type="button"
                    className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    onClick={() => setBusqueda("")}
                  >
                    <X size={13} />
                  </button>
                )}
              </div>
            </div>

            <div className="flex-1 overflow-y-auto px-3 py-2">
              {productosFiltrados.length === 0 ? (
                <div className="text-center py-8 text-gray-400 text-sm">Sin productos</div>
              ) : (
                <div className="flex flex-col gap-0.5">
                  {productosFiltrados.map((p) => (
                    <ProductoRow
                      key={p.id}
                      producto={p}
                      cantidad={cantidades[p.id] ?? 0}
                      onChange={(n) => cambiarCantidad(p.id, n)}
                    />
                  ))}
                </div>
              )}

              <div className="mt-4 pt-3 border-t border-gray-100">
                <div className="text-[11px] font-semibold text-gray-400 uppercase tracking-wider mb-2">Ítem manual / no catalogado</div>
                <form onSubmit={(e) => { e.preventDefault(); agregarManual(); }} className="flex gap-2 items-center">
                  <input
                    className="form-control flex-1 text-sm"
                    placeholder="Descripción"
                    value={manual.nombre}
                    onChange={(e) => setManual((m) => ({ ...m, nombre: e.target.value }))}
                  />
                  <input
                    className="form-control w-16 text-sm text-center"
                    type="number" min="1" placeholder="Cant."
                    value={manual.cantidad}
                    onChange={(e) => setManual((m) => ({ ...m, cantidad: e.target.value }))}
                  />
                  <input
                    className="form-control w-28 text-sm"
                    type="number" min="0" placeholder="Precio unitario COP"
                    value={manual.precio}
                    onChange={(e) => setManual((m) => ({ ...m, precio: e.target.value }))}
                  />
                  <button type="submit" className="btn btn-secondary btn-sm shrink-0" title="Agregar">
                    <Plus size={14} />
                  </button>
                </form>
              </div>
            </div>
          </div>

          {/* Derecha: ítems a agregar */}
          <div className="flex-1 flex flex-col overflow-hidden bg-gray-50/40">
            <div className="px-5 py-3 border-b border-gray-200 shrink-0">
              <span className="font-bold text-sm text-gray-600">Ítems a agregar</span>
            </div>

            <div className="flex-1 overflow-y-auto px-4 py-3">
              {seleccionados.length === 0 && manualItems.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-gray-300 select-none">
                  <Utensils size={44} className="mb-3 opacity-25" />
                  <p className="text-sm font-medium">Sin ítems seleccionados</p>
                  <p className="text-xs mt-1">Usa el catálogo de la izquierda</p>
                </div>
              ) : (
                <div className="space-y-1">
                  {seleccionados.map((p) => (
                    <div key={p.id} className="flex items-center gap-2.5 py-1.5">
                      <span className="w-6 h-6 rounded-md bg-verde-claro text-white flex items-center justify-center text-[11px] font-bold shrink-0">{p.cantidad}</span>
                      <span className="flex-1 text-sm font-medium text-gray-800 truncate">{p.nombre}</span>
                      <span className="text-sm font-semibold text-verde-oscuro shrink-0">{formatCOP(p.precio * p.cantidad)}</span>
                      <button className="btn-icon text-red-400 shrink-0" title="Quitar" onClick={() => cambiarCantidad(p.id, 0)}>
                        <X size={14} />
                      </button>
                    </div>
                  ))}
                  {manualItems.map((m) => (
                    <div key={m._key} className="flex items-center gap-2.5 py-1.5">
                      <span className="w-6 h-6 rounded-md bg-gray-400 text-white flex items-center justify-center text-[11px] font-bold shrink-0">{m.cantidad}</span>
                      <span className="flex-1 text-sm italic text-gray-600 truncate">{m.nombre}</span>
                      <span className="text-sm font-semibold shrink-0">{formatCOP((parseFloat(m.precio) || 0) * m.cantidad)}</span>
                      <button className="btn-icon text-red-400 shrink-0" onClick={() => setManualItems((ms) => ms.filter((x) => x._key !== m._key))}>
                        <X size={14} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {totalItems > 0 && (
              <div className="px-5 py-3 border-t border-gray-200 bg-white shrink-0">
                <div className="text-xs text-gray-400 mb-0.5">Ítems a agregar</div>
                <div className="text-lg font-black text-verde-oscuro">{totalItems} ítem{totalItems !== 1 ? "s" : ""}</div>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-white shrink-0">
          <div className="flex items-center gap-3">
            <button className="btn btn-secondary" onClick={onCancelar}>Cancelar</button>
            {error && <span className="text-red-500 text-sm">{error}</span>}
          </div>
          <button
            className="btn btn-primary px-6 py-2.5 text-[15px] font-bold"
            onClick={handleConfirmar}
            disabled={totalItems === 0}
          >
            {`✅ Confirmar · ${totalItems} ítem${totalItems !== 1 ? "s" : ""}`}
          </button>
        </div>
      </div>
    </div>
  );
}
