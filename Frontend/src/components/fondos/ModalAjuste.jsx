import { useState } from "react";
import { X } from "lucide-react";
import { formatCOP, formatUSD } from "../../utils/format";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import MoneyInput from "../shared/MoneyInput";
import ConfirmDialog from "../shared/ConfirmDialog";

/** Modal de ajuste manual de fondo. Extraído de FondosPage. */
export default function ModalAjuste({ fondo, onClose, onAjustar }) {
  const [nuevoSaldo, setNuevoSaldo] = useState(fondo.saldo ?? 0);
  const [motivo, setMotivo]         = useState("");
  const [error, setError]           = useState("");
  const [saving, setSaving]         = useState(false);
  const [confirmando, setConfirmando] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const handleSubmit = (e) => {
    e.preventDefault();
    setError("");
    if (nuevoSaldo === "" || isNaN(Number(nuevoSaldo))) {
      setError("Ingresa un saldo válido");
      return;
    }
    setConfirmando(true);
  };

  const handleConfirmar = async () => {
    setConfirmando(false);
    setSaving(true);
    try {
      await onAjustar(fondo.moneda, Number(nuevoSaldo), motivo);
      onClose();
    } catch (err) {
      setError(err.message);
      setSaving(false);
    }
  };

  const diferencia = Number(nuevoSaldo) - (fondo.saldo ?? 0);
  const fmtSaldo = (v) => fondo.moneda === "USD" ? `${formatUSD(v)} USD` : formatCOP(v);

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[420px]">
        <div className="modal-header">
          <span className="modal-title">Ajustar fondo {fondo.moneda}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && <div className="alert alert-error">{error}</div>}

            <div className="form-group">
              <label className="form-label">
                Saldo actual: <strong>
                  {fondo.moneda === "USD"
                    ? `${formatUSD(fondo.saldo ?? 0)} USD`
                    : formatCOP(fondo.saldo ?? 0)}
                </strong>
              </label>
              <MoneyInput
                className="form-control"
                moneda={fondo.moneda}
                value={String(nuevoSaldo)}
                onChange={setNuevoSaldo}
                autoFocus
              />
              {diferencia !== 0 && (
                <div className={`mt-1.5 text-[13px] ${diferencia > 0 ? "text-verde-claro" : "text-red-600"}`}>
                  {diferencia > 0 ? "+" : ""}{fondo.moneda === "USD"
                    ? `${formatUSD(diferencia)} USD`
                    : formatCOP(diferencia)}
                </div>
              )}
            </div>

            <div className="form-group">
              <label className="form-label">Motivo (opcional)</label>
              <input
                className="form-control"
                value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                placeholder="ej. Capital inicial, corrección de caja"
              />
            </div>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Guardando…" : "Aplicar ajuste"}
            </button>
          </div>
        </form>
      </div>

      {confirmando && (
        <ConfirmDialog
          emoji="⚖️"
          titulo={`Ajustar fondo ${fondo.moneda}`}
          descripcion={`El saldo pasará de ${fmtSaldo(fondo.saldo ?? 0)} a ${fmtSaldo(Number(nuevoSaldo))} (${diferencia >= 0 ? "+" : ""}${fmtSaldo(diferencia)}). Este movimiento queda registrado en el libro de caja.`}
          labelOk="Sí, aplicar ajuste"
          onOk={handleConfirmar}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}
