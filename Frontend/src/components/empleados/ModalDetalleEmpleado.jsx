import { useState } from "react";
import { X, Trash2 } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { empleadoService } from "../../services/empleadoService";
import { fondoService } from "../../services/fondoService";
import { formatCOP, formatCreadoEn } from "../../utils/format";
import ModalLiquidar from "./ModalLiquidar";

const hoy = () => new Date().toLocaleDateString("en-CA");

const calcDias = (inicio, fin) =>
  Math.round((new Date(fin) - new Date(inicio)) / 86400000) + 1;

export default function ModalDetalleEmpleado({ empleado, onClose }) {
  const qc = useQueryClient();
  const [tab, setTab] = useState("adelantos");
  const [liquidando, setLiquidando] = useState(false);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const [adelantoForm, setAdelantoForm] = useState({
    monto: "", moneda: "COP", fecha: hoy(), observacion: "", tipo: "EFECTIVO",
  });
  const [diaForm, setDiaForm] = useState({ fecha: hoy(), observacion: "" });
  const [vacForm, setVacForm] = useState({
    fechaInicio: hoy(), fechaFin: hoy(), observacion: "",
  });

  useModalKeyboard({ onEscape: onClose });

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const { data: adelantos = [], isLoading: loadAdelantos } = useQuery({
    queryKey: ["empleado-adelantos", empleado.id],
    queryFn: () => empleadoService.listarAdelantos(empleado.id),
  });

  const { data: dias = [], isLoading: loadDias } = useQuery({
    queryKey: ["empleado-dias", empleado.id],
    queryFn: () => empleadoService.listarDias(empleado.id),
    enabled: empleado.tipo === "POR_DIA",
  });

  const { data: pagos = [], isLoading: loadPagos } = useQuery({
    queryKey: ["empleado-pagos", empleado.id],
    queryFn: () => empleadoService.listarPagos(empleado.id),
  });

  const { data: vacaciones = [], isLoading: loadVacaciones } = useQuery({
    queryKey: ["empleado-vacaciones", empleado.id],
    queryFn: () => empleadoService.listarVacaciones(empleado.id),
  });

  const mutAdelanto = useMutation({
    mutationFn: (data) => empleadoService.registrarAdelanto(empleado.id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["empleado-adelantos", empleado.id] });
      setAdelantoForm({ monto: "", moneda: "COP", fecha: hoy(), observacion: "", tipo: "EFECTIVO" });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutRegistrarDia = useMutation({
    mutationFn: (data) => empleadoService.registrarDia(empleado.id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["empleado-dias", empleado.id] });
      setDiaForm({ fecha: hoy(), observacion: "" });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutEliminarDia = useMutation({
    mutationFn: (diaId) => empleadoService.eliminarDia(empleado.id, diaId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleado-dias", empleado.id] }),
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutVacacion = useMutation({
    mutationFn: (data) => empleadoService.registrarVacacion(empleado.id, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["empleado-vacaciones", empleado.id] });
      setVacForm({ fechaInicio: hoy(), fechaFin: hoy(), observacion: "" });
      setError("");
    },
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const mutEliminarVac = useMutation({
    mutationFn: (vacId) => empleadoService.eliminarVacacion(empleado.id, vacId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["empleado-vacaciones", empleado.id] }),
    onError: (err) => setError(err.response?.data?.message ?? err.message),
  });

  const handleRegistrarAdelanto = async (e) => {
    e.preventDefault();
    if (!adelantoForm.monto || Number(adelantoForm.monto) <= 0) {
      setError("El monto debe ser mayor a 0"); return;
    }
    if (adelantoForm.tipo === "ESPECIE" && !adelantoForm.observacion?.trim()) {
      setError("La observación es obligatoria para descuentos en especie"); return;
    }
    setSaving(true);
    try {
      await mutAdelanto.mutateAsync({
        monto: Number(adelantoForm.monto),
        moneda: adelantoForm.tipo === "ESPECIE" ? "COP" : adelantoForm.moneda,
        fecha: adelantoForm.fecha,
        observacion: adelantoForm.observacion || null,
        tipo: adelantoForm.tipo,
      });
    } finally { setSaving(false); }
  };

  const handleRegistrarDia = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await mutRegistrarDia.mutateAsync({
        fecha: diaForm.fecha,
        observacion: diaForm.observacion || null,
      });
    } finally { setSaving(false); }
  };

  const handleRegistrarVacacion = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      await mutVacacion.mutateAsync({
        fechaInicio: vacForm.fechaInicio,
        fechaFin: vacForm.fechaFin,
        observacion: vacForm.observacion || null,
      });
    } finally { setSaving(false); }
  };

  const diasPendientes = dias.filter((d) => !d.pagado);
  const estimadoDias = diasPendientes.length * Number(empleado.salario);

  const TABS = [
    { id: "adelantos", label: "Adelantos" },
    ...(empleado.tipo === "POR_DIA" ? [{ id: "dias", label: "Días trabajados" }] : []),
    { id: "pagos", label: "Pagos" },
    { id: "vacaciones", label: "Vacaciones" },
  ];

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">{empleado.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}

          <div className="tabs mb-4">
            {TABS.map((t) => (
              <button
                key={t.id}
                className={`tab ${tab === t.id ? "activo" : ""}`}
                onClick={() => { setTab(t.id); setError(""); }}
              >
                {t.label}
              </button>
            ))}
          </div>

          {tab === "adelantos" && (
            <div>
              {loadAdelantos ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : adelantos.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Fecha</th><th>Monto</th><th>Tipo</th><th>Fondo</th><th>Observación</th><th>Estado</th></tr>
                    </thead>
                    <tbody>
                      {adelantos.map((a) => (
                        <tr key={a.id}>
                          <td>{a.fecha}</td>
                          <td>{Number(a.monto).toLocaleString("es-CO")} {a.tipo !== "ESPECIE" ? a.moneda : ""}</td>
                          <td>
                            {a.tipo === "ESPECIE"
                              ? <span className="badge badge-gray">Especie</span>
                              : <span className="badge badge-info">Efectivo</span>}
                          </td>
                          <td>{a.tipo === "ESPECIE" ? "—" : a.fondoDebitado}</td>
                          <td className="text-xs text-gray-500">{a.observacion ?? "—"}</td>
                          <td>
                            <span className={`badge ${a.pagado ? "badge-success" : "badge-warning"}`}>
                              {a.pagado ? "Pagado" : "Pendiente"}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <form onSubmit={handleRegistrarAdelanto} className="border-t pt-4">
                <p className="text-sm font-medium mb-3">Registrar adelanto o descuento</p>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Tipo *</label>
                    <select
                      className="form-control"
                      value={adelantoForm.tipo}
                      onChange={(e) => setAdelantoForm((f) => ({ ...f, tipo: e.target.value }))}
                    >
                      <option value="EFECTIVO">Efectivo (debita fondo)</option>
                      <option value="ESPECIE">En especie (mercado, gallina, etc.)</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Monto COP *</label>
                    <input
                      type="number" className="form-control" min="1" step="1"
                      value={adelantoForm.monto}
                      onChange={(e) => setAdelantoForm((f) => ({ ...f, monto: e.target.value }))}
                      placeholder="0"
                    />
                  </div>
                  {adelantoForm.tipo === "EFECTIVO" && (
                    <div className="form-group">
                      <label className="form-label">Fondo *</label>
                      <select
                        className="form-control"
                        value={adelantoForm.moneda}
                        onChange={(e) => setAdelantoForm((f) => ({ ...f, moneda: e.target.value }))}
                      >
                        {fondos.map((f) => (
                          <option key={f.moneda} value={f.moneda}>{f.alias ?? f.moneda}</option>
                        ))}
                      </select>
                    </div>
                  )}
                  <div className="form-group">
                    <label className="form-label">Fecha *</label>
                    <input
                      type="date" className="form-control"
                      value={adelantoForm.fecha}
                      onChange={(e) => setAdelantoForm((f) => ({ ...f, fecha: e.target.value }))}
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">
                    Observación {adelantoForm.tipo === "ESPECIE" ? "*" : ""}
                  </label>
                  <input
                    className="form-control"
                    value={adelantoForm.observacion}
                    onChange={(e) => setAdelantoForm((f) => ({ ...f, observacion: e.target.value }))}
                    placeholder={adelantoForm.tipo === "ESPECIE" ? "Ej: mercado 8.000, gallina 45.000" : "Opcional"}
                  />
                </div>
                <div className="flex justify-end mt-2">
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? "Registrando…" : "Registrar"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {tab === "dias" && (
            <div>
              <div className="flex items-center gap-4 mb-3 text-sm bg-gray-50 rounded p-3">
                <span>
                  <strong>{diasPendientes.length}</strong> días pendientes —{" "}
                  <strong>{formatCOP(estimadoDias)}</strong> estimado
                </span>
              </div>
              {loadDias ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : dias.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Fecha</th><th>Observación</th><th>Estado</th><th></th></tr>
                    </thead>
                    <tbody>
                      {dias.map((d) => (
                        <tr key={d.id}>
                          <td>{d.fecha}</td>
                          <td>{d.observacion ?? "—"}</td>
                          <td>
                            <span className={`badge ${d.pagado ? "badge-success" : "badge-warning"}`}>
                              {d.pagado ? "Pagado" : "Pendiente"}
                            </span>
                          </td>
                          <td>
                            {!d.pagado && (
                              <button
                                className="btn-icon"
                                title="Eliminar"
                                onClick={() => mutEliminarDia.mutate(d.id)}
                              >
                                <Trash2 size={14} className="text-red-500" />
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <form onSubmit={handleRegistrarDia} className="border-t pt-4">
                <p className="text-sm font-medium mb-3">Registrar día trabajado</p>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Fecha *</label>
                    <input
                      type="date" className="form-control"
                      value={diaForm.fecha}
                      onChange={(e) => setDiaForm((f) => ({ ...f, fecha: e.target.value }))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Observación</label>
                    <input
                      className="form-control"
                      value={diaForm.observacion}
                      onChange={(e) => setDiaForm((f) => ({ ...f, observacion: e.target.value }))}
                      placeholder="Opcional"
                    />
                  </div>
                </div>
                <div className="flex justify-end mt-2">
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? "Registrando…" : "Registrar día"}
                  </button>
                </div>
              </form>
            </div>
          )}

          {tab === "pagos" && (
            <div>
              {loadPagos ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : pagos.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr>
                        <th>Fecha</th><th>Periodo</th><th>Bruto</th>
                        <th>Adelantos</th><th>Neto</th><th>Moneda</th><th>Fondo</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pagos.map((p) => (
                        <tr key={p.id}>
                          <td className="text-xs">{p.creadoEn ? formatCreadoEn(p.creadoEn) : "—"}</td>
                          <td className="text-xs">{p.periodoInicio} / {p.periodoFin}</td>
                          <td>{formatCOP(p.salarioBruto)}</td>
                          <td>{formatCOP(p.totalAdelantos)}</td>
                          <td className="font-medium">{formatCOP(p.totalNeto)}</td>
                          <td>{p.moneda}</td>
                          <td>{p.fondoDebitado}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <div className="flex justify-end border-t pt-4">
                <button className="btn btn-primary" onClick={() => setLiquidando(true)}>
                  Registrar pago
                </button>
              </div>
            </div>
          )}

          {tab === "vacaciones" && (
            <div>
              {loadVacaciones ? (
                <div className="loading-center"><div className="spinner" /></div>
              ) : vacaciones.length > 0 ? (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Inicio</th><th>Fin</th><th>Días</th><th>Observación</th><th></th></tr>
                    </thead>
                    <tbody>
                      {vacaciones.map((v) => (
                        <tr key={v.id}>
                          <td>{v.fechaInicio}</td>
                          <td>{v.fechaFin}</td>
                          <td>{calcDias(v.fechaInicio, v.fechaFin)}</td>
                          <td>{v.observacion ?? "—"}</td>
                          <td>
                            <button
                              className="btn-icon"
                              title="Eliminar"
                              onClick={() => mutEliminarVac.mutate(v.id)}
                            >
                              <Trash2 size={14} className="text-red-500" />
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
              <form onSubmit={handleRegistrarVacacion} className="border-t pt-4">
                <p className="text-sm font-medium mb-3">Registrar periodo de vacaciones</p>
                <div className="form-row">
                  <div className="form-group">
                    <label className="form-label">Fecha inicio *</label>
                    <input
                      type="date" className="form-control"
                      value={vacForm.fechaInicio}
                      onChange={(e) => setVacForm((f) => ({ ...f, fechaInicio: e.target.value }))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Fecha fin *</label>
                    <input
                      type="date" className="form-control"
                      value={vacForm.fechaFin}
                      onChange={(e) => setVacForm((f) => ({ ...f, fechaFin: e.target.value }))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Observación</label>
                    <input
                      className="form-control"
                      value={vacForm.observacion}
                      onChange={(e) => setVacForm((f) => ({ ...f, observacion: e.target.value }))}
                      placeholder="Opcional"
                    />
                  </div>
                </div>
                <div className="flex justify-end mt-2">
                  <button type="submit" className="btn btn-primary" disabled={saving}>
                    {saving ? "Registrando…" : "Registrar vacaciones"}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
        </div>
      </div>

      {liquidando && (
        <ModalLiquidar
          empleado={empleado}
          onClose={() => setLiquidando(false)}
          onSuccess={() => {
            qc.invalidateQueries({ queryKey: ["empleado-pagos", empleado.id] });
            qc.invalidateQueries({ queryKey: ["empleado-adelantos", empleado.id] });
            qc.invalidateQueries({ queryKey: ["empleado-dias", empleado.id] });
            setLiquidando(false);
          }}
        />
      )}
    </div>
  );
}
