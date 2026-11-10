import { useState } from "react";
import { X } from "lucide-react";
import { formatDecimal } from "../../utils/format";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/** Modal para registrar un movimiento de inventario. Extraído de InventarioPage. */
export default function ModalMovimiento({ insumos, onClose, onSave }) {
  const [form, setForm] = useState({
    insumo: insumos[0]?.insumo ?? "gallina",
    tipo: "entrada",
    cantidad: "",
    observacion: "",
  });
  const [error, setError]   = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    const cantidad = parseFloat(form.cantidad);
    if (isNaN(cantidad) || cantidad <= 0) { setError("La cantidad debe ser mayor a 0"); return; }
    setSaving(true);
    try {
      await onSave({ ...form, cantidad });
      onClose();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const stockActual = insumos.find((i) => i.insumo === form.insumo)?.stockActual ?? 0;

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar movimiento de inventario</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Insumo</label>
                <select className="form-control" value={form.insumo}
                  onChange={(e) => set("insumo", e.target.value)}>
                  {insumos.map((i) => <option key={i.insumo} value={i.insumo}>{i.insumo}</option>)}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">Tipo</label>
                <select className="form-control" value={form.tipo}
                  onChange={(e) => set("tipo", e.target.value)}>
                  <option value="entrada">Entrada (compra/ingreso)</option>
                  <option value="salida">Salida (manual)</option>
                  <option value="ajuste">Ajuste (nuevo stock exacto)</option>
                </select>
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">
                Cantidad{form.tipo === "ajuste" ? " (stock final)" : ""}
                {" "}— Stock actual: <strong>{formatDecimal(stockActual)}</strong>
              </label>
              <input className="form-control" type="number" step="0.001" min="0"
                value={form.cantidad} onChange={(e) => set("cantidad", e.target.value)} autoFocus />
            </div>
            <div className="form-group">
              <label className="form-label">Observación (opcional)</label>
              <input className="form-control" value={form.observacion}
                onChange={(e) => set("observacion", e.target.value)} />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Registrar"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
