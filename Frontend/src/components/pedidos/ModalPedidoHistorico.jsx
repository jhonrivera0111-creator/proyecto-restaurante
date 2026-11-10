import { useState } from "react";
import { Plus, Search, Trash2, Utensils, X } from "lucide-react";
import { formatCOP } from "../../utils/format";
import { normalizarFracciones, toTitleCase } from "../../utils/pedidoHelpers";
import MoneyInput from "../shared/MoneyInput";
import ProductoRow from "./ProductoRow";
import ConfirmDialog from "../shared/ConfirmDialog";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

const hoy = () => new Date().toLocaleDateString("en-CA");

/**
 * Carga retroactiva de un pedido ya ocurrido (empalme de registros previos al
 * uso de Casa Verde). A diferencia de "Nuevo pedido", no es una reserva ni
 * pasa por el ciclo pendiente→pagado en tiempo real: se crea, cobra y
 * finaliza de una — afecta fondos e inventario reales, fechado en el día que
 * se indique. Solo ADMIN (gateado en PedidosPage).
 */
export default function ModalPedidoHistorico({ productos, onClose, onCrear }) {
  const [nombre, setNombre]         = useState("");
  const [fecha, setFecha]           = useState(hoy());
  const [observacion, setObservacion] = useState("");
  const [cantidades, setCantidades] = useState({});
  const [manual, setManual]         = useState({ nombre: "", precio: "", cantidad: 1 });
  const [manualItems, setManualItems] = useState([]);
  const [busqueda, setBusqueda]     = useState("");
  const [pagos, setPagos]           = useState([{ moneda: "COP", monto: "" }]);
  const [confirmCancelar, setConfirmCancelar] = useState(false);
  const [error, setError]           = useState("");
  const [saving, setSaving]         = useState(false);

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

  const total = seleccionados.reduce((s, p) => s + p.precio * p.cantidad, 0)
    + manualItems.reduce((s, m) => s + (parseFloat(m.precio) || 0) * m.cantidad, 0);

  const totalPagado = pagos.reduce((s, p) => s + (parseFloat(p.monto) || 0), 0);
  const descuadrado = totalItems > 0 && totalPagado !== total;

  const setPago = (i, k, v) => setPagos((ps) => ps.map((p, idx) => idx === i ? { ...p, [k]: v } : p));
  const agregarPago = () => setPagos((ps) => [...ps, { moneda: "COP", monto: "" }]);
  const quitarPago = (i) => setPagos((ps) => ps.filter((_, idx) => idx !== i));

  const handleCrear = async () => {
    if (!nombre.trim()) { setError("El nombre del cliente es obligatorio"); return; }
    if (!fecha) { setError("La fecha es obligatoria"); return; }
    if (totalItems === 0) { setError("Agrega al menos un ítem"); return; }
    if (descuadrado) { setError("Los pagos deben sumar exactamente el total — el pedido se finaliza de inmediato"); return; }
    setSaving(true); setError("");
    try {
      const detalles = [
        ...seleccionados.map((p) => ({ productoId: p.id, cantidad: p.cantidad })),
        ...manualItems.map((m) => ({
          nombreManual: m.nombre.trim(),
          precioManual: parseFloat(m.precio) || 0,
          cantidad: m.cantidad,
        })),
      ];
      await onCrear({
        nombre: nombre.trim(),
        fecha,
        observacion: observacion.trim() || null,
        detalles,
        pagos: pagos
          .filter((p) => (parseFloat(p.monto) || 0) > 0)
          .map((p) => ({ moneda: p.moneda, monto: parseFloat(p.monto) })),
      });
      onClose();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  useModalKeyboard({ onEscape: () => setConfirmCancelar(true) });

  return (
    <>
      <div className="fixed inset-0 bg-black/45 z-[1000] flex items-center justify-center p-4 animate-fade-in">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl flex flex-col overflow-hidden animate-slide-up" style={{ height: "90vh" }}>

          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0 bg-amber-50">
            <div>
              <h2 className="text-amber-700 font-extrabold text-xl">📋 Carga histórica</h2>
              <p className="text-[12px] text-amber-700/80 mt-0.5">
                Crea, cobra y finaliza de una — afecta fondos e inventario reales, fechado retroactivamente
              </p>
            </div>
            <button className="btn-icon" onClick={() => setConfirmCancelar(true)}><X size={20} /></button>
          </div>

          <div className="flex flex-1 overflow-hidden">

            <div className="flex flex-col border-r border-gray-200 overflow-hidden" style={{ width: "58%" }}>
              <div className="px-5 py-3 border-b border-gray-100 shrink-0 space-y-2">
                {error && <div className="alert alert-error">{error}</div>}
                <div className="flex gap-2">
                  <input
                    className="form-control text-[15px] font-semibold flex-1"
                    placeholder="Nombre del cliente *"
                    value={nombre}
                    onChange={(e) => setNombre(toTitleCase(e.target.value))}
                    autoFocus
                  />
                  <input
                    type="date"
                    className="form-control w-40"
                    value={fecha}
                    onChange={(e) => setFecha(e.target.value)}
                  />
                </div>
                <textarea
                  className="form-control text-sm resize-none"
                  placeholder="Observación (opcional)"
                  rows={2}
                  value={observacion}
                  onChange={(e) => setObservacion(e.target.value)}
                />
              </div>

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
                    <button type="submit" className="btn btn-secondary btn-sm shrink-0" title="Agregar ítem">
                      <Plus size={14} />
                    </button>
                  </form>
                </div>
              </div>
            </div>

            <div className="flex-1 flex flex-col overflow-hidden bg-gray-50/40">
              <div className="px-5 py-3 border-b border-gray-200 shrink-0">
                <span className="font-bold text-sm text-gray-600">Resumen</span>
              </div>
              <div className="flex-1 overflow-y-auto px-4 py-3">
                {seleccionados.length === 0 && manualItems.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-gray-300 select-none">
                    <Utensils size={44} className="mb-3 opacity-25" />
                    <p className="text-sm font-medium">Sin productos seleccionados</p>
                  </div>
                ) : (
                  <div className="space-y-1">
                    {seleccionados.map((p) => (
                      <div key={p.id} className="flex items-center gap-2.5 py-1.5">
                        <span className="w-6 h-6 rounded-md bg-verde-claro text-white flex items-center justify-center text-[11px] font-bold shrink-0">{p.cantidad}</span>
                        <span className="flex-1 text-sm font-medium text-gray-800 truncate">{p.nombre}</span>
                        <span className="text-sm font-semibold text-verde-oscuro shrink-0">{formatCOP(p.precio * p.cantidad)}</span>
                      </div>
                    ))}
                    {manualItems.map((m) => (
                      <div key={m._key} className="flex items-center gap-2.5 py-1.5">
                        <span className="w-6 h-6 rounded-md bg-gray-400 text-white flex items-center justify-center text-[11px] font-bold shrink-0">{m.cantidad}</span>
                        <span className="flex-1 text-sm italic text-gray-600 truncate">{m.nombre}</span>
                        <span className="text-sm font-semibold shrink-0">{formatCOP((parseFloat(m.precio) || 0) * m.cantidad)}</span>
                        <button className="btn-icon text-red-400 shrink-0" onClick={() => setManualItems((ms) => ms.filter((x) => x._key !== m._key))}>
                          <X size={13} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {totalItems > 0 && (
                <div className="border-t border-gray-200 bg-white shrink-0">
                  <div className="px-5 pt-3 pb-1">
                    <div className="flex justify-between items-end pt-1 pb-2">
                      <div>
                        <div className="text-xs text-gray-400 mb-0.5">Total</div>
                        <div className="text-[24px] font-black text-verde-oscuro leading-none">{formatCOP(total)}</div>
                      </div>
                      <div className="text-right">
                        <div className="text-xs text-gray-400">Ítems</div>
                        <div className="text-lg font-bold text-gray-700">{totalItems}</div>
                      </div>
                    </div>
                  </div>
                  <div className="px-5 pb-3">
                    <div className="text-[12px] font-semibold text-verde-oscuro mb-1.5">
                      Pago(s) — debe sumar exacto (se finaliza de inmediato)
                    </div>
                    <div className="flex flex-col gap-1.5">
                      {pagos.map((p, i) => (
                        <div key={i} className="flex gap-2">
                          <select
                            className="form-control text-sm w-28"
                            value={p.moneda}
                            onChange={(e) => setPago(i, "moneda", e.target.value)}
                          >
                            <option value="COP">COP</option>
                            <option value="USD">USD</option>
                            <option value="NEQUI">Nequi</option>
                          </select>
                          <MoneyInput
                            className="form-control flex-1 text-sm"
                            moneda={p.moneda}
                            placeholder="Monto"
                            value={p.monto}
                            onChange={(v) => setPago(i, "monto", v)}
                          />
                          {pagos.length > 1 && (
                            <button type="button" className="btn-icon text-red-400" onClick={() => quitarPago(i)}>
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                    <button type="button" className="text-[12px] text-verde-claro font-semibold mt-1.5" onClick={agregarPago}>
                      + Agregar otro pago
                    </button>
                    <div className={`text-[12px] mt-2 font-medium ${descuadrado ? "text-red-500" : "text-verde-claro"}`}>
                      Pagado: {formatCOP(totalPagado)} {descuadrado && `(faltan ${formatCOP(total - totalPagado)})`}
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-white shrink-0">
            <button className="btn btn-secondary" onClick={() => setConfirmCancelar(true)}>Cancelar</button>
            <button
              className="btn btn-primary px-6 py-2.5 text-[15px] font-bold"
              onClick={handleCrear}
              disabled={saving || !nombre.trim() || totalItems === 0 || descuadrado}
            >
              {saving ? "Guardando…" : `📋 Registrar pedido histórico`}
            </button>
          </div>
        </div>
      </div>

      {confirmCancelar && (
        <ConfirmDialog
          emoji="🗑️"
          titulo="¿Cancelar la carga histórica?"
          descripcion="No se creará ningún registro."
          labelOk="Sí, cancelar"
          peligro
          onOk={onClose}
          onCancelar={() => setConfirmCancelar(false)}
        />
      )}
    </>
  );
}
