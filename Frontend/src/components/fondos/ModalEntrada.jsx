import { useState } from "react";
import { X } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { useQuery } from "@tanstack/react-query";
import { fondoService } from "../../services/fondoService";
import MoneyInput from "../shared/MoneyInput";

export default function ModalEntrada({ onClose, onGuardar }) {
  const [form, setForm] = useState({
    concepto: "", moneda: "COP", monto: "",
  });
  const [error, setError]   = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });
  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  const { data: fondos = [] } = useQuery({ queryKey: ["fondos"], queryFn: fondoService.listar });
  const nequiCuentas = fondos.filter((f) => f.moneda.startsWith("NEQUI"));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.concepto.trim()) { setError("El concepto es obligatorio"); return; }
    if (!form.monto || Number(form.monto) <= 0) { setError("El monto debe ser mayor a cero"); return; }
    setSaving(true);
    try {
      await onGuardar({ ...form, monto: Number(form.monto) });
      onClose();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[440px]">
        <div className="modal-header">
          <span className="modal-title">Nueva entrada</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-group">
              <label className="form-label">Concepto</label>
              <input
                className="form-control" autoFocus
                value={form.concepto}
                onChange={(e) => set("concepto", e.target.value)}
                placeholder="ej. Préstamo socio, Capital adicional…"
              />
            </div>

            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Moneda</label>
                <select className="form-control" value={form.moneda}
                  onChange={(e) => set("moneda", e.target.value)}>
                  <option value="COP">COP — Banco</option>
                  <option value="USD">USD — Dólares</option>
                  <option value="EFECTIVO">Efectivo — Caja física</option>
                  {nequiCuentas.map((f) => (
                    <option key={f.moneda} value={f.moneda}>
                      {f.moneda}{f.alias ? ` — ${f.alias}` : ""}
                    </option>
                  ))}
                </select>
              </div>
              <div className="form-group">
                <label className="form-label">
                  Monto {form.moneda === "USD" ? "(en dólares)" : "(en pesos)"}
                </label>
                <MoneyInput
                  className="form-control"
                  moneda={form.moneda}
                  value={form.monto}
                  onChange={(v) => set("monto", v)}
                />
              </div>
            </div>


          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Registrar entrada"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
