import { useState } from "react";
import { Plus, Search, Utensils, Wallet, X } from "lucide-react";
import { formatCOP } from "../../utils/format";
import { normalizarFracciones, toTitleCase } from "../../utils/pedidoHelpers";
import MoneyInput from "../shared/MoneyInput";
import ToggleChip from "../shared/ToggleChip";
import ProductoRow from "./ProductoRow";
import ConfirmDialog from "../shared/ConfirmDialog";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/** Modal de creación de pedido: datos del cliente + catálogo + abono inicial. */
export default function ModalCrearPedido({ salones, productos, onClose, onCrear }) {
  const [nombre, setNombre]         = useState("Sr. ");
  const [on, setOn]                 = useState({ fecha: false, hora: false, personas: false, salon: false, telefono: false });
  const [fecha, setFecha]           = useState(new Date().toLocaleDateString("en-CA"));
  const [hora, setHora]             = useState(new Date().toTimeString().slice(0, 5));
  const [personas, setPersonas]     = useState(1);
  const [salonId, setSalonId]       = useState("");
  const [telefono, setTelefono]     = useState("");
  const [paraLlevar, setParaLlevar] = useState(false);
  const [observacion, setObservacion] = useState("");
  const [cantidades, setCantidades] = useState({});
  // Orden de agregado (cola): ids de productos en el orden en que el usuario
  // los fue seleccionando, para que "Resumen del pedido" no salga en el
  // orden alfabético/categoría del catálogo sino en el orden real de armado.
  const [ordenIds, setOrdenIds]     = useState([]);
  const [manual, setManual]         = useState({ nombre: "", precio: "", cantidad: 1 });
  const [manualItems, setManualItems] = useState([]);
  const [busqueda, setBusqueda]     = useState("");
  const [showAbono, setShowAbono]   = useState(false);
  const [abonoMoneda, setAbonoMoneda] = useState("COP");
  const [abonoMonto, setAbonoMonto] = useState("");
  const [showDescuento, setShowDescuento] = useState(false);
  const [descuentoCop, setDescuentoCop]   = useState("");
  const [confirmCancelar, setConfirmCancelar] = useState(false);
  const [error, setError]           = useState("");
  const [saving, setSaving]         = useState(false);

  const tog = (k) => setOn((o) => {
    const next = { ...o, [k]: !o[k] };
    if (k === "fecha" && next.fecha) next.hora = true;
    if (k === "hora"  && !next.hora) next.fecha = false;
    return next;
  });

  const cambiarCantidad = (id, n) =>
    setCantidades((c) => {
      const updated = n <= 0
        ? (() => { const { [id]: _, ...rest } = c; return rest; })()
        : { ...c, [id]: n };
      const normalizadas = normalizarFracciones(updated, productos);
      setOrdenIds((prev) => {
        const presentes = new Set(Object.keys(normalizadas).map(Number).filter((pid) => normalizadas[pid] > 0));
        const mantenidos = prev.filter((pid) => presentes.has(pid));
        const nuevos = [...presentes].filter((pid) => !prev.includes(pid));
        return [...mantenidos, ...nuevos];
      });
      return normalizadas;
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

  const seleccionados = ordenIds
    .map((id) => productos.find((p) => p.id === id))
    .filter(Boolean)
    .map((p) => ({ ...p, cantidad: cantidades[p.id] }));

  const totalItems = seleccionados.reduce((s, p) => s + p.cantidad, 0)
    + manualItems.reduce((s, m) => s + m.cantidad, 0);

  const totalEstimado = seleccionados.reduce((s, p) => s + p.precio * p.cantidad, 0)
    + manualItems.reduce((s, m) => s + (parseFloat(m.precio) || 0) * m.cantidad, 0);
  const descuentoNum   = showDescuento && descuentoCop ? parseFloat(descuentoCop) || 0 : 0;
  const totalConDescuento = Math.max(0, totalEstimado - descuentoNum);

  const handleCrear = async () => {
    if (!nombre.trim()) { setError("El nombre del cliente es obligatorio"); return; }
    if (totalItems === 0) { setError("Agrega al menos un ítem al pedido"); return; }
    if (descuentoNum > totalEstimado) { setError("El descuento no puede ser mayor al subtotal"); return; }
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
        nombre:       nombre.trim(),
        salonId:      on.salon && salonId ? Number(salonId) : null,
        fechaReserva: on.fecha ? fecha : (on.hora ? new Date().toLocaleDateString("en-CA") : null),
        horaReserva:  on.hora    ? hora    : null,
        cantPersonas: on.personas ? Number(personas) : null,
        telefono:     on.telefono && telefono.trim() ? telefono.trim() : null,
        paraLlevar:   paraLlevar || null,
        observacion:  observacion.trim() || null,
        abono:        showAbono && abonoMonto ? parseFloat(abonoMonto) : null,
        monedaAbono:  showAbono && abonoMonto ? abonoMoneda : null,
        descuentoCop: descuentoNum > 0 ? descuentoNum : null,
        detalles,
      });
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  // Esc abre confirm cancelar; Enter crea el pedido (salvo que esté guardando o focus esté en form de ítems manuales)
  useModalKeyboard({ onEscape: () => setConfirmCancelar(true), onEnter: saving ? undefined : handleCrear });

  return (
    <>
      <div className="fixed inset-0 bg-black/45 z-[1000] flex items-center justify-center p-4 animate-fade-in">
        <div className="bg-white rounded-2xl shadow-2xl w-full max-w-5xl flex flex-col overflow-hidden animate-slide-up" style={{ height: "90vh" }}>

          {/* Header */}
          <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 shrink-0">
            <h2 className="text-verde-claro font-extrabold text-xl">Nuevo Pedido</h2>
            <button className="btn-icon" onClick={() => setConfirmCancelar(true)}><X size={20} /></button>
          </div>

          {/* Body */}
          <div className="flex flex-1 overflow-hidden">

            {/* Izquierda: cliente + catálogo */}
            <div className="flex flex-col border-r border-gray-200 overflow-hidden" style={{ width: "58%" }}>
              <div className="px-5 py-3 border-b border-gray-100 shrink-0 space-y-2">
                {error && <div className="alert alert-error">{error}</div>}
                <input
                  className={`form-control text-[15px] font-semibold ${!nombre.trim() && error ? "border-red-500" : ""}`}
                  placeholder="Nombre del cliente *"
                  value={nombre}
                  onChange={(e) => setNombre(toTitleCase(e.target.value))}
                  autoFocus
                />
                <div className="flex flex-wrap gap-1.5">
                  <ToggleChip label="🕐 Reserva"    active={on.hora}     onToggle={() => tog("hora")} />
                  <ToggleChip label="👥 Personas"   active={on.personas} onToggle={() => tog("personas")} />
                  <ToggleChip label="🪑 Salón"      active={on.salon}    onToggle={() => tog("salon")} />
                  <ToggleChip label="🛵 Para llevar" active={paraLlevar}  onToggle={() => setParaLlevar((v) => !v)} />
                  <ToggleChip label="📞 Teléfono"   active={on.telefono} onToggle={() => tog("telefono")} />
                </div>
                <textarea
                  className="form-control text-sm resize-none"
                  placeholder="Observación del pedido (opcional)"
                  rows={2}
                  value={observacion}
                  onChange={(e) => setObservacion(e.target.value)}
                />
                {(on.hora || on.personas || on.salon || on.telefono) && (
                  <div className="flex flex-wrap gap-2 pt-1 items-center">
                    {on.hora && (
                      <>
                        <input type="time" className="form-control min-w-[110px]" value={hora} onChange={(e) => setHora(e.target.value)} />
                        <ToggleChip label="📅 + Fecha" active={on.fecha} onToggle={() => tog("fecha")} />
                        {on.fecha && <input type="date" className="form-control flex-1 min-w-[140px]" value={fecha} onChange={(e) => setFecha(e.target.value)} />}
                      </>
                    )}
                    {on.personas && <input type="number" min="1" placeholder="Personas" className="form-control w-28" value={personas} onChange={(e) => setPersonas(e.target.value)} />}
                    {on.salon    && (
                      <select className="form-control flex-1" value={salonId} onChange={(e) => setSalonId(e.target.value)}>
                        <option value="">Salón…</option>
                        {salones.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
                      </select>
                    )}
                    {on.telefono && (
                      <input
                        type="tel"
                        placeholder="Teléfono del cliente"
                        className="form-control flex-1 min-w-[160px]"
                        maxLength={30}
                        value={telefono}
                        onChange={(e) => setTelefono(e.target.value)}
                      />
                    )}
                  </div>
                )}
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

            {/* Derecha: resumen + abono */}
            <div className="flex-1 flex flex-col overflow-hidden bg-gray-50/40">
              <div className="px-5 py-3 border-b border-gray-200 shrink-0">
                <span className="font-bold text-sm text-gray-600">Resumen del pedido</span>
              </div>
              <div className="flex-1 overflow-y-auto px-4 py-3">
                {seleccionados.length === 0 && manualItems.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-full text-gray-300 select-none">
                    <Utensils size={44} className="mb-3 opacity-25" />
                    <p className="text-sm font-medium">Sin productos seleccionados</p>
                    <p className="text-xs mt-1">Usa el catálogo de la izquierda</p>
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
                    {descuentoNum > 0 && (
                      <>
                        <div className="flex justify-between text-[13px] text-gray-500">
                          <span>Subtotal</span><span>{formatCOP(totalEstimado)}</span>
                        </div>
                        <div className="flex justify-between text-[13px] text-red-500">
                          <span>Descuento</span><span>-{formatCOP(descuentoNum)}</span>
                        </div>
                      </>
                    )}
                    <div className="flex justify-between items-end pt-1 pb-2">
                      <div>
                        <div className="text-xs text-gray-400 mb-0.5">{descuentoNum > 0 ? "Total a pagar" : "Total estimado"}</div>
                        <div className="text-[24px] font-black text-verde-oscuro leading-none">{formatCOP(totalConDescuento)}</div>
                      </div>
                      <div className="text-right">
                        <div className="text-xs text-gray-400">Ítems</div>
                        <div className="text-lg font-bold text-gray-700">{totalItems}</div>
                      </div>
                    </div>
                  </div>
                  <div className="px-5 pb-3 flex flex-col gap-2">
                    {!showAbono ? (
                      <button className="btn btn-secondary btn-sm w-full justify-center text-gray-500" onClick={() => setShowAbono(true)}>
                        <Wallet size={13} /> Registrar abono inicial
                      </button>
                    ) : (
                      <div className="bg-gray-50 rounded-xl p-3 border border-gray-200">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-[12px] font-semibold text-verde-oscuro">Abono inicial</span>
                          <button className="btn-icon text-gray-400" onClick={() => { setShowAbono(false); setAbonoMonto(""); }}>
                            <X size={14} />
                          </button>
                        </div>
                        <div className="flex gap-2">
                          <select className="form-control text-sm w-28" value={abonoMoneda} onChange={(e) => setAbonoMoneda(e.target.value)}>
                            <option value="COP">COP</option>
                            <option value="USD">USD</option>
                            <option value="NEQUI">Nequi</option>
                          </select>
                          <MoneyInput
                            className="form-control flex-1 text-sm"
                            moneda={abonoMoneda}
                            placeholder="Monto"
                            value={abonoMonto}
                            onChange={setAbonoMonto}
                          />
                        </div>
                      </div>
                    )}
                    {!showDescuento ? (
                      <button className="btn btn-secondary btn-sm w-full justify-center text-gray-500" onClick={() => setShowDescuento(true)}>
                        🏷️ Aplicar descuento
                      </button>
                    ) : (
                      <div className="bg-gray-50 rounded-xl p-3 border border-gray-200">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-[12px] font-semibold text-verde-oscuro">Descuento (COP)</span>
                          <button className="btn-icon text-gray-400" onClick={() => { setShowDescuento(false); setDescuentoCop(""); }}>
                            <X size={14} />
                          </button>
                        </div>
                        <MoneyInput
                          className="form-control text-sm"
                          moneda="COP"
                          placeholder="Monto del descuento"
                          value={descuentoCop}
                          onChange={setDescuentoCop}
                        />
                        {descuentoNum > totalEstimado && (
                          <div className="text-[12px] text-red-500 mt-1">El descuento no puede ser mayor al subtotal</div>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-white shrink-0">
            <button className="btn btn-secondary" onClick={() => setConfirmCancelar(true)}>Cancelar</button>
            <button
              className="btn btn-primary px-6 py-2.5 text-[15px] font-bold"
              onClick={handleCrear}
              disabled={saving || !nombre.trim() || descuentoNum > totalEstimado}
            >
              {saving ? "Creando…" : `🍽️ Crear pedido${totalItems > 0 ? ` · ${totalItems} ítem${totalItems !== 1 ? "s" : ""}` : ""}`}
            </button>
          </div>
        </div>
      </div>

      {confirmCancelar && (
        <ConfirmDialog
          emoji="🗑️"
          titulo="¿Cancelar el pedido?"
          descripcion={totalItems > 0
            ? `Perderás ${totalItems} ítem${totalItems !== 1 ? "s" : ""} seleccionado${totalItems !== 1 ? "s" : ""}. No se creará ningún registro.`
            : "¿Seguro que deseas cerrar sin crear el pedido?"}
          labelOk="Sí, cancelar"
          peligro
          onOk={onClose}
          onCancelar={() => setConfirmCancelar(false)}
        />
      )}
    </>
  );
}
