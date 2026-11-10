import { useState } from "react";
import { Plus, X, AlertTriangle } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { formatCOP } from "../../utils/format";

const FRACCIONES = {
  gallina: [
    { valor: 0.125, label: "⅛ gallina" },
    { valor: 0.25,  label: "¼ gallina" },
    { valor: 0.5,   label: "½ gallina" },
    { valor: 1,     label: "1 gallina entera" },
  ],
  cuy: [
    { valor: 0.25,  label: "¼ cuy" },
    { valor: 0.5,   label: "½ cuy" },
    { valor: 1,     label: "1 cuy entero" },
  ],
};

export default function ModalProducto({ producto, categorias, onClose, onSave }) {
  const [form, setForm] = useState({
    nombre:           producto?.nombre           ?? "",
    precio:           producto?.precio           ?? 0,
    categoriaId:      producto?.categoriaId      ?? "",
    afectaInventario: producto?.afectaInventario ?? false,
    composicion:      producto?.composicion      ?? [],
  });
  const [error, setError]   = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const agregarComp = () =>
    setForm((f) => ({ ...f, composicion: [...f.composicion, { insumo: "gallina", cantidad: 0.125 }] }));

  const quitarComp = (i) =>
    setForm((f) => ({ ...f, composicion: f.composicion.filter((_, idx) => idx !== i) }));

  const setComp = (i, k, v) =>
    setForm((f) => {
      const comp = [...f.composicion];
      if (k === "insumo") {
        const primerValor = FRACCIONES[v]?.[0]?.valor ?? 0.5;
        comp[i] = { ...comp[i], insumo: v, cantidad: primerValor };
      } else {
        comp[i] = { ...comp[i], [k]: v };
      }
      return { ...f, composicion: comp };
    });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.nombre.trim()) { setError("El nombre es obligatorio"); return; }
    setSaving(true);
    try {
      await onSave({
        ...form,
        precio:      parseFloat(form.precio) || 0,
        categoriaId: form.categoriaId ? Number(form.categoriaId) : null,
        composicion: form.afectaInventario
          ? form.composicion.map((c) => ({ ...c, cantidad: parseFloat(c.cantidad) || 0 }))
          : [],
      });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">{producto ? "Editar producto" : "Nuevo producto"}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Nombre *</label>
                <input
                  className="form-control"
                  value={form.nombre}
                  onChange={(e) => set("nombre", e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">
                  Precio (COP)
                  {producto && (
                    <span className="ml-2 text-[12px] font-normal text-gray-400">
                      actual: {formatCOP(producto.precio)}
                    </span>
                  )}
                </label>
                <input
                  className="form-control"
                  type="number"
                  min="0"
                  step="1"
                  placeholder="Ej: 25000"
                  value={form.precio}
                  onChange={(e) => set("precio", e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Categoría</label>
              <select
                className="form-control"
                value={form.categoriaId}
                onChange={(e) => set("categoriaId", e.target.value)}
              >
                <option value="">— Sin categoría —</option>
                {categorias.map((c) => (
                  <option key={c.id} value={c.id}>{c.nombre}</option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <div className="flex items-center gap-2.5">
                <input
                  type="checkbox"
                  id="afecta"
                  checked={form.afectaInventario}
                  onChange={(e) => set("afectaInventario", e.target.checked)}
                />
                <label htmlFor="afecta" className="cursor-pointer font-medium">
                  ¿Este producto descuenta inventario al venderse?
                </label>
              </div>
            </div>

            {form.afectaInventario && (
              <div className="border border-gray-200 rounded-lg p-3 bg-gray-50">
                <div className="flex items-center justify-between mb-1">
                  <div>
                    <p className="font-semibold text-[13px] text-verde-oscuro">
                      ¿Qué se descuenta al vender 1 unidad?
                    </p>
                    <p className="text-[12px] text-gray-500">
                      Cada cantidad se descuenta proporcional a las unidades vendidas.
                    </p>
                  </div>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm shrink-0"
                    onClick={agregarComp}
                  >
                    <Plus size={13} /> Agregar insumo
                  </button>
                </div>

                {form.composicion.length === 0 ? (
                  <div className="flex items-center gap-2 mt-2 p-2 bg-yellow-50 border border-yellow-200 rounded text-[12px] text-yellow-800">
                    <AlertTriangle size={14} className="shrink-0" />
                    Sin insumos — no se descontará inventario aunque esté marcado
                  </div>
                ) : (
                  <div className="flex flex-col gap-1.5 mt-2">
                    {form.composicion.map((c, i) => (
                      <div key={i} className="flex items-center gap-2">
                        <select
                          className="form-control flex-1"
                          value={c.insumo}
                          onChange={(e) => setComp(i, "insumo", e.target.value)}
                        >
                          <option value="gallina">Gallina</option>
                          <option value="cuy">Cuy</option>
                        </select>
                        <select
                          className="form-control w-[140px]"
                          value={c.cantidad}
                          onChange={(e) => setComp(i, "cantidad", parseFloat(e.target.value))}
                        >
                          {(FRACCIONES[c.insumo] ?? []).map((f) => (
                            <option key={f.valor} value={f.valor}>{f.label}</option>
                          ))}
                        </select>
                        <button
                          type="button"
                          className="btn-icon shrink-0"
                          onClick={() => quitarComp(i)}
                        >
                          <X size={15} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>
              Cancelar
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Guardar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
