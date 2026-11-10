import { useState } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { empleadoService } from "../../services/empleadoService";
import { fondoService } from "../../services/fondoService";
import { formatCOP } from "../../utils/format";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalLiquidar({ empleado, onClose, onSuccess }) {
  const [form, setForm] = useState({
    moneda: "COP",
    periodoInicio: hoy(),
    periodoFin: hoy(),
    observacion: "",
  });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  useModalKeyboard({ onEscape: onClose });

  const { data: preview, isLoading: loadPreview } = useQuery({
    queryKey: ["empleado-preview", empleado.id],
    queryFn: () => empleadoService.previewPago(empleado.id),
  });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const mutLiquidar = useMutation({
    mutationFn: (data) => empleadoService.liquidar(empleado.id, data),
    onSuccess: () => onSuccess(),
    onError: (err) => {
      setError(err.response?.data?.message ?? err.message);
      setSaving(false);
    },
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.periodoInicio || !form.periodoFin) {
      setError("El periodo es obligatorio"); return;
    }
    setSaving(true);
    try {
      await mutLiquidar.mutateAsync({
        moneda: form.moneda,
        periodoInicio: form.periodoInicio,
        periodoFin: form.periodoFin,
        observacion: form.observacion || null,
      });
    } catch {
      // error handled in onError
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Registrar pago — {empleado.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}

          {loadPreview ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : preview ? (
            <div className="bg-gray-50 rounded p-3 mb-4 text-sm">
              <p className="font-medium mb-2">Resumen del pago</p>
              <div className="grid grid-cols-2 gap-2">
                {empleado.tipo === "POR_DIA" && (
                  <div>
                    <span className="text-gray-500">Días pendientes:</span>{" "}
                    <strong>{preview.totalDiasPendientes}</strong>
                  </div>
                )}
                <div>
                  <span className="text-gray-500">Salario bruto:</span>{" "}
                  <strong>{formatCOP(preview.salarioBrutoEstimado)}</strong>
                </div>
                <div>
                  <span className="text-gray-500">Adelantos a descontar:</span>{" "}
                  <strong className="text-amber-700">{formatCOP(preview.totalAdelantosCop)}</strong>
                </div>
                <div>
                  <span className="text-gray-500">Neto a pagar:</span>{" "}
                  <strong className={Number(preview.netoEstimado) < 0 ? "text-red-600" : "text-green-700"}>
                    {formatCOP(preview.netoEstimado)}
                  </strong>
                </div>
              </div>
              {preview.adelantosPendientes.length > 0 && (
                <p className="text-xs text-gray-500 mt-2">
                  {preview.adelantosPendientes.length} adelanto(s) pendiente(s) se descontarán automáticamente
                </p>
              )}
            </div>
          ) : null}

          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Periodo inicio *</label>
                <input
                  type="date" className="form-control"
                  value={form.periodoInicio}
                  onChange={(e) => setForm((f) => ({ ...f, periodoInicio: e.target.value }))}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Periodo fin *</label>
                <input
                  type="date" className="form-control"
                  value={form.periodoFin}
                  onChange={(e) => setForm((f) => ({ ...f, periodoFin: e.target.value }))}
                />
              </div>
            </div>
            <div className="form-group">
              <label className="form-label">Pagar desde *</label>
              <select
                className="form-control"
                value={form.moneda}
                onChange={(e) => setForm((f) => ({ ...f, moneda: e.target.value }))}
              >
                {fondos.map((f) => (
                  <option key={f.moneda} value={f.moneda}>{f.alias ?? f.moneda}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label className="form-label">Observación</label>
              <input
                className="form-control"
                value={form.observacion}
                onChange={(e) => setForm((f) => ({ ...f, observacion: e.target.value }))}
                placeholder="Opcional"
              />
            </div>
            <div className="flex justify-end gap-2 mt-4">
              <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={saving || loadPreview || (preview && Number(preview.netoEstimado) < 0)}
              >
                {saving ? "Procesando…" : "Confirmar pago"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
