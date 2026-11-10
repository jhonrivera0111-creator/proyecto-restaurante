import { useState, useMemo } from "react";
import { Plus, X } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { proveedorService } from "../../services/proveedorService";
import { compraService } from "../../services/compraService";
import { formatCOP } from "../../utils/format";
import MoneyInput from "../shared/MoneyInput";
import ModalProveedor from "./ModalProveedor";

const hoy = () => new Date().toLocaleDateString("en-CA");

export default function ModalRegistrarCompra({ onClose }) {
  const qc = useQueryClient();
  const [form, setForm] = useState({
    proveedorId: "",
    insumo: "gallina",
    cantidad: "",
    precioUnitario: "",
    fecha: hoy(),
    observacion: "",
  });
  const [showAbono, setShowAbono] = useState(false);
  const [abonoMoneda, setAbonoMoneda] = useState("COP");
  const [abonoMonto, setAbonoMonto] = useState("");
  const [creandoProveedor, setCreandoProveedor] = useState(false);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  // Compra ya persistida (id) cuando el abono inicial falla después de crearla
  // — evita duplicar la compra si el usuario reintenta: el segundo submit
  // solo reintenta el abono contra esta misma compra.
  const [compraCreada, setCompraCreada] = useState(null);

  useModalKeyboard({ onEscape: onClose });

  const { data: proveedores = [] } = useQuery({
    queryKey: ["proveedores", true],
    queryFn: () => proveedorService.listar(true),
  });

  const total = useMemo(() => {
    const c = parseFloat(form.cantidad);
    const p = parseFloat(form.precioUnitario);
    return !isNaN(c) && !isNaN(p) ? c * p : 0;
  }, [form.cantidad, form.precioUnitario]);

  const mutCrearProveedor = useMutation({
    mutationFn: proveedorService.crear,
    onSuccess: (nuevo) => {
      qc.invalidateQueries({ queryKey: ["proveedores"] });
      setForm((f) => ({ ...f, proveedorId: String(nuevo.id) }));
      setCreandoProveedor(false);
    },
  });

  const mutRegistrar = useMutation({
    mutationFn: compraService.registrar,
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutAbono = useMutation({
    mutationFn: ({ id, data }) => compraService.registrarAbono(id, data),
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    let compra = compraCreada;
    if (!compra) {
      if (!form.proveedorId) { setError("Selecciona un proveedor"); return; }
      if (!form.cantidad || parseFloat(form.cantidad) <= 0) { setError("La cantidad debe ser mayor a 0"); return; }
      if (!form.precioUnitario || parseFloat(form.precioUnitario) <= 0) { setError("El precio debe ser mayor a 0"); return; }
    }
    const abonoNum = showAbono ? parseFloat(abonoMonto) || 0 : 0;
    if (abonoNum > total) { setError("El abono no puede ser mayor al total de la compra"); return; }

    setSaving(true);
    try {
      if (!compra) {
        compra = await mutRegistrar.mutateAsync({
          proveedorId: Number(form.proveedorId),
          insumo: form.insumo,
          cantidad: parseFloat(form.cantidad),
          precioUnitario: parseFloat(form.precioUnitario),
          fecha: form.fecha,
          observacion: form.observacion.trim() || null,
        });
        setCompraCreada(compra);
      }
      if (abonoNum > 0) {
        await mutAbono.mutateAsync({
          id: compra.id,
          data: { monto: abonoNum, moneda: abonoMoneda, fecha: form.fecha },
        });
      }
      qc.invalidateQueries({ queryKey: ["compras"] });
      qc.invalidateQueries({ queryKey: ["inventario-stock"] });
      qc.invalidateQueries({ queryKey: ["inventario-movimientos"] });
      qc.invalidateQueries({ queryKey: ["fondos"] });
      onClose();
    } catch (err) {
      const msg = err.response?.data?.message ?? err.message;
      setError(compra ? `Compra registrada. El abono no se pudo aplicar: ${msg}` : msg);
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div className="modal-overlay">
        <div className="modal">
          <div className="modal-header">
            <span className="modal-title">Registrar compra</span>
            <button className="btn-icon" onClick={onClose}><X size={18} /></button>
          </div>
          <form onSubmit={handleSubmit}>
            <div className="modal-body">
              {error && <div className="alert alert-error">{error}</div>}
              {compraCreada && (
                <div className="alert" style={{ background: "#e8f4ec", color: "#2d6a4f" }}>
                  Compra #{compraCreada.id} registrada. Completa el abono para continuar.
                </div>
              )}
              <fieldset disabled={!!compraCreada} className="contents">
              <div className="form-group">
                <label className="form-label" htmlFor="compra-proveedor">Proveedor *</label>
                <div className="flex gap-2">
                  <select
                    id="compra-proveedor"
                    className="form-control flex-1"
                    value={form.proveedorId}
                    onChange={(e) => setForm((f) => ({ ...f, proveedorId: e.target.value }))}
                  >
                    <option value="">Seleccionar…</option>
                    {proveedores.map((p) => (
                      <option key={p.id} value={p.id}>{p.nombre}</option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm shrink-0"
                    title="Nuevo proveedor"
                    onClick={() => setCreandoProveedor(true)}
                  >
                    <Plus size={14} />
                  </button>
                </div>
                {proveedores.length === 0 && (
                  <p className="text-[12px] text-gray-400 mt-1">Sin proveedores — crea uno con el botón +</p>
                )}
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="compra-insumo">Insumo *</label>
                  <select
                    id="compra-insumo"
                    className="form-control"
                    value={form.insumo}
                    onChange={(e) => setForm((f) => ({ ...f, insumo: e.target.value }))}
                  >
                    <option value="gallina">Gallina</option>
                    <option value="cuy">Cuy</option>
                  </select>
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="compra-fecha">Fecha *</label>
                  <input
                    id="compra-fecha"
                    type="date"
                    className="form-control"
                    value={form.fecha}
                    onChange={(e) => setForm((f) => ({ ...f, fecha: e.target.value }))}
                  />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label" htmlFor="compra-cantidad">Cantidad *</label>
                  <input
                    id="compra-cantidad"
                    type="number"
                    className="form-control"
                    min="0.25"
                    step="0.25"
                    value={form.cantidad}
                    onChange={(e) => setForm((f) => ({ ...f, cantidad: e.target.value }))}
                    placeholder="0"
                  />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="compra-precio">Precio unitario COP *</label>
                  <input
                    id="compra-precio"
                    type="number"
                    className="form-control"
                    min="1"
                    step="1"
                    value={form.precioUnitario}
                    onChange={(e) => setForm((f) => ({ ...f, precioUnitario: e.target.value }))}
                    placeholder="0"
                  />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="compra-observacion">Observación</label>
                <input
                  id="compra-observacion"
                  className="form-control"
                  value={form.observacion}
                  onChange={(e) => setForm((f) => ({ ...f, observacion: e.target.value }))}
                  placeholder="Opcional"
                />
              </div>
              <div className="flex justify-between items-center pt-2 border-t border-gray-100">
                <span className="text-sm text-gray-500">Total estimado</span>
                <span className="font-bold text-lg">{formatCOP(total)}</span>
              </div>
              </fieldset>

              {!showAbono ? (
                <button
                  type="button"
                  className="btn btn-secondary btn-sm w-full justify-center text-gray-500 mt-2"
                  onClick={() => setShowAbono(true)}
                >
                  💰 Registrar abono inicial
                </button>
              ) : (
                <div className="bg-gray-50 rounded-xl p-3 border border-gray-200 mt-2">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-[12px] font-semibold text-verde-oscuro">Abono inicial</span>
                    <button
                      type="button"
                      className="btn-icon text-gray-400"
                      onClick={() => { setShowAbono(false); setAbonoMonto(""); }}
                    >
                      <X size={14} />
                    </button>
                  </div>
                  <div className="flex gap-2">
                    <select
                      className="form-control text-sm w-28"
                      value={abonoMoneda}
                      onChange={(e) => setAbonoMoneda(e.target.value)}
                    >
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
            </div>
            <div className="modal-footer">
              <button type="button" className="btn btn-secondary" onClick={onClose}>
                {compraCreada ? "Cerrar" : "Cancelar"}
              </button>
              <button type="submit" className="btn btn-primary" disabled={saving}>
                {saving ? "Guardando…" : compraCreada ? "Reintentar abono" : "Registrar compra"}
              </button>
            </div>
          </form>
        </div>
      </div>

      {creandoProveedor && (
        <ModalProveedor
          onClose={() => setCreandoProveedor(false)}
          onSave={(data) => mutCrearProveedor.mutateAsync(data)}
        />
      )}
    </>
  );
}
