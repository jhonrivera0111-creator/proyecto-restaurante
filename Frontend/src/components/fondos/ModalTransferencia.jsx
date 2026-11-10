import { useState } from "react";
import { X, ArrowRight, AlertTriangle, CheckCircle, Info } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { useQuery } from "@tanstack/react-query";
import { tasaService } from "../../services/tasaService";
import { fondoService } from "../../services/fondoService";
import { formatCOP, formatUSD } from "../../utils/format";
import MoneyInput from "../shared/MoneyInput";

/** GMF sobre la porción gravable (monto que excede el cupo libre). */
function calcGMF(montoGravable) {
  return Math.floor(Number(montoGravable) * 4 / 1000);
}

/** Porción del retiro sujeta al 4x1000. */
function montoGravable(monto, disponible) {
  if (disponible <= 0) return monto;
  return Math.max(0, monto - disponible);
}

function getMonedaLabel(moneda, fondos) {
  const fondo = fondos.find((f) => f.moneda === moneda);
  if (moneda === "COP") return "COP — Banco";
  if (moneda === "USD") return "USD — Dólares";
  if (fondo?.alias) return `${moneda} — ${fondo.alias}`;
  return moneda;
}

export default function ModalTransferencia({ fondos = [], onClose, onGuardar, monedaOrigenInicial, monedaDestinoDefault, titulo, conceptoFijo }) {
  // Toda billetera puede transferir a cualquier otra. Cuando se abre desde
  // una tarjeta puntual, el origen queda fijo (así se pidió la operación);
  // el destino siempre es editable, solo se pre-selecciona un valor por
  // defecto sensato (Efectivo, salvo que el origen ya sea Efectivo → Banco).
  const origenBloqueado = !!monedaOrigenInicial;

  const todasMonedas = [
    ...fondos.filter((f) => f.moneda === "COP").map((f) => f.moneda),
    ...fondos.filter((f) => f.moneda === "USD").map((f) => f.moneda),
    ...fondos.filter((f) => f.moneda === "EFECTIVO").map((f) => f.moneda),
    ...fondos.filter((f) => f.moneda.startsWith("NEQUI")).map((f) => f.moneda),
  ];

  const nequiDefault = fondos.find((f) => f.moneda.startsWith("NEQUI"))?.moneda
    ?? todasMonedas[0] ?? "COP";

  const origenInicial = monedaOrigenInicial ?? nequiDefault;

  const [form, setForm] = useState({
    monedaOrigen:  origenInicial,
    montoOrigen:   "",
    monedaDestino: monedaDestinoDefault ?? (todasMonedas.find((m) => m !== origenInicial) ?? ""),
    montoDestino:  "",
    concepto:      "",
    observacion:   "",
  });
  const [error,  setError]  = useState("");
  const [saving, setSaving] = useState(false);

  const { data: tasa } = useQuery({
    queryKey: ["tasa-vigente"],
    queryFn: tasaService.vigente,
    retry: false,
  });
  const tasaValor = tasa?.valorCop ?? null;

  const esNequiOrigen = form.monedaOrigen.startsWith("NEQUI");

  const { data: estadoNequi } = useQuery({
    queryKey: ["nequi-estado-mes", form.monedaOrigen],
    queryFn: () => fondoService.estadoNequiMes(form.monedaOrigen),
    enabled: esNequiOrigen,
  });

  useModalKeyboard({ onEscape: onClose });

  const set = (k, v) => setForm((f) => ({ ...f, [k]: v }));

  /* ── Determinación automática del 4x1000 ──────────────────────────
     Aplica si: el límite está configurado > 0
                Y (el cupo ya está agotado  O  el monto supera lo disponible)
  ─────────────────────────────────────────────────────────────────── */
  const montoNum        = Number(form.montoOrigen) || 0;
  const limiteExento    = estadoNequi ? Number(estadoNequi.limiteExento)    : 0;
  const disponibleLibre = estadoNequi ? Number(estadoNequi.disponibleLibre) : Infinity;

  const aplicar4x1000Auto = esNequiOrigen &&
    limiteExento > 0 &&
    estadoNequi !== undefined &&
    (disponibleLibre <= 0 || montoNum > disponibleLibre);

  /* ── Calcula monto destino dado un monto origen (sin depender del estado) ── */
  const calcMontoDestino = (monto) => {
    if (!monto || monto <= 0) return "";
    const aplicar = esNequiOrigen && limiteExento > 0 && estadoNequi !== undefined &&
      (disponibleLibre <= 0 || monto > disponibleLibre);
    if (aplicar) {
      const gravable = montoGravable(monto, disponibleLibre);
      const neto = monto - calcGMF(gravable);
      if (form.monedaDestino === "USD" && tasaValor) return (neto / tasaValor).toFixed(2);
      return String(neto);
    }
    const esUsdSalida  = form.monedaOrigen === "USD" && form.monedaDestino !== "USD";
    const esUsdEntrada = form.monedaDestino === "USD" && form.monedaOrigen !== "USD";
    if (esUsdSalida  && tasaValor) return String(Math.round(monto * tasaValor));
    if (esUsdEntrada && tasaValor) return (monto / tasaValor).toFixed(2);
    return String(monto);
  };

  /* ── Cambio de cuenta origen ─────────────────────────────────────── */
  const handleOrigenChange = (v) => {
    setForm((f) => {
      const destino = f.monedaDestino === v
        ? todasMonedas.find((m) => m !== v) ?? ""
        : f.monedaDestino;
      return { ...f, monedaOrigen: v, monedaDestino: destino, montoDestino: "" };
    });
  };

  /* ── Actualización en tiempo real del monto destino ─────────────── */
  const handleMontoOrigenChange = (v) => {
    const monto = Number(v) || 0;
    setForm((f) => ({ ...f, montoOrigen: v, montoDestino: calcMontoDestino(monto) }));
  };

  /* ── GMF en tiempo real (solo sobre el excedente del cupo libre) ── */
  const gravableNum = aplicar4x1000Auto && montoNum > 0
    ? montoGravable(montoNum, disponibleLibre)
    : 0;
  const gmfAmount = gravableNum > 0 ? calcGMF(gravableNum) : null;

  /* ── Tasa efectiva (solo USD) ────────────────────────────────────── */
  const tasaEfectiva = (() => {
    if (aplicar4x1000Auto) return null;
    const d = Number(form.montoDestino);
    if (!montoNum || !d) return null;
    if (form.monedaOrigen === "USD" && form.monedaDestino !== "USD")
      return `1 USD = ${Math.round(d / montoNum).toLocaleString("es-CO")} ${form.monedaDestino}`;
    if (form.monedaDestino === "USD" && form.monedaOrigen !== "USD")
      return `1 USD = ${Math.round(montoNum / d).toLocaleString("es-CO")} ${form.monedaOrigen}`;
    return null;
  })();

  const saldoOrigen = fondos.find((f) => f.moneda === form.monedaOrigen)?.saldo ?? null;

  /* ── Estado visual del indicador 4x1000 ─────────────────────────── */
  const indicador4x1000 = (() => {
    if (!esNequiOrigen || !estadoNequi || limiteExento <= 0) return null;

    if (disponibleLibre <= 0) {
      const gmfTotal = montoNum > 0 ? calcGMF(montoNum) : null;
      return { color: "bg-red-50 text-red-700", Icon: AlertTriangle,
        texto: `Cupo mensual agotado — todo el retiro (${formatCOP(montoNum)}) está gravado` +
               (gmfTotal ? ` · 4×1000 estimado: ${formatCOP(gmfTotal)}` : "") };
    }
    if (montoNum > 0 && montoNum > disponibleLibre) {
      const excedente = montoNum - disponibleLibre;
      const gmfParcial = calcGMF(excedente);
      return { color: "bg-orange-50 text-orange-700", Icon: AlertTriangle,
        texto: `Libre de impuesto: ${formatCOP(disponibleLibre)} · Gravado: ${formatCOP(excedente)} · 4×1000 estimado: ${formatCOP(gmfParcial)}` };
    }
    return { color: "bg-green-50 text-green-800", Icon: CheckCircle,
      texto: `Dentro del cupo libre: ${formatCOP(disponibleLibre)} disponibles de ${formatCOP(limiteExento)} — sin 4×1000` };
  })();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.montoOrigen || montoNum <= 0) {
      setError("El monto de origen debe ser mayor a cero"); return;
    }
    if (!form.montoDestino || Number(form.montoDestino) <= 0) {
      setError("El monto de destino debe ser mayor a cero"); return;
    }
    setSaving(true);
    try {
      await onGuardar({
        monedaOrigen:  form.monedaOrigen,
        montoOrigen:   montoNum,
        monedaDestino: form.monedaDestino,
        montoDestino:  Number(form.montoDestino),
        concepto:      conceptoFijo ?? (form.concepto.trim() || undefined),
        observacion:   conceptoFijo ? undefined : form.observacion.trim() || undefined,
        aplicar4x1000: aplicar4x1000Auto || undefined,
      });
      onClose();
    } catch (err) {
      setError(err.response?.data?.message ?? err.message);
      setSaving(false);
    }
  };

  const destinosDisponibles = todasMonedas.filter((m) => m !== form.monedaOrigen);

  return (
    <div className="modal-overlay">
      <div className="modal max-w-[500px]">
        <div className="modal-header">
          <span className="modal-title">{titulo ?? "Transferencia entre fondos"}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="modal-body flex flex-col gap-4">
            {error && <div className="alert alert-error">{error}</div>}

            {/* Origen → Destino */}
            <div className="flex items-end gap-3">
              <div className="form-group flex-1 mb-0">
                <label className="form-label">Fondo origen</label>
                {origenBloqueado ? (
                  <div className="form-control bg-gray-50 text-gray-700 cursor-default">
                    {getMonedaLabel(form.monedaOrigen, fondos)}
                  </div>
                ) : (
                  <select className="form-control" value={form.monedaOrigen}
                    onChange={(e) => handleOrigenChange(e.target.value)}>
                    {todasMonedas.map((m) => (
                      <option key={m} value={m}>{getMonedaLabel(m, fondos)}</option>
                    ))}
                  </select>
                )}
                {saldoOrigen !== null && (
                  <p className="text-[11px] text-gray-400 mt-1">
                    Disponible: {form.monedaOrigen === "USD"
                      ? formatUSD(saldoOrigen)
                      : formatCOP(saldoOrigen)}
                  </p>
                )}
              </div>
              <div className="pb-[10px] text-gray-400"><ArrowRight size={20} /></div>
              <div className="form-group flex-1 mb-0">
                <label className="form-label">Fondo destino</label>
                <select className="form-control" value={form.monedaDestino}
                  onChange={(e) => set("monedaDestino", e.target.value)}>
                  {destinosDisponibles.map((m) => (
                    <option key={m} value={m}>{getMonedaLabel(m, fondos)}</option>
                  ))}
                </select>
              </div>
            </div>

            {/* Indicador automático 4x1000 */}
            {indicador4x1000 && (
              <div className={`text-[12px] rounded px-3 py-2 flex items-start gap-2 ${indicador4x1000.color}`}>
                <indicador4x1000.Icon size={14} className="mt-px shrink-0" />
                <span>{indicador4x1000.texto}</span>
              </div>
            )}

            {/* Montos */}
            <div className="flex items-end gap-3">
              <div className="form-group flex-1 mb-0">
                <label className="form-label">
                  Monto a retirar {form.monedaOrigen === "USD" ? "(USD)" : "(pesos)"}
                </label>
                <MoneyInput
                  className="form-control"
                  moneda={form.monedaOrigen}
                  value={form.montoOrigen}
                  onChange={handleMontoOrigenChange}
                  autoFocus
                />
                {gmfAmount !== null && gmfAmount > 0 && (
                  <p className="text-[11px] text-orange-600 mt-1">
                    Gravado: {formatCOP(gravableNum)} · 4×1000: −{formatCOP(gmfAmount)} · neto: {formatCOP(montoNum - gmfAmount)}
                  </p>
                )}
              </div>
              <div className="pb-[10px] text-gray-400"><ArrowRight size={20} /></div>
              <div className="form-group flex-1 mb-0">
                <label className="form-label">
                  Monto a acreditar {form.monedaDestino === "USD" ? "(USD)" : "(pesos)"}
                </label>
                <MoneyInput
                  className={`form-control ${esNequiOrigen ? "bg-gray-50 text-gray-500 cursor-default" : ""}`}
                  moneda={form.monedaDestino}
                  value={form.montoDestino}
                  onChange={esNequiOrigen ? undefined : (v) => set("montoDestino", v)}
                  readOnly={esNequiOrigen}
                />
              </div>
            </div>

            {/* Tasa efectiva (USD) */}
            {tasaEfectiva && (
              <div className="text-[12px] text-gray-500 bg-gray-50 rounded px-3 py-2">
                Tasa efectiva: <span className="font-semibold text-gray-700">{tasaEfectiva}</span>
                {tasaValor && (
                  <span className="ml-2 text-gray-400">(sistema: {Math.round(tasaValor).toLocaleString("es-CO")})</span>
                )}
              </div>
            )}

            {/* Nota informativa si NEQUI sin límite configurado */}
            {esNequiOrigen && limiteExento === 0 && estadoNequi && (
              <div className="text-[12px] text-gray-500 bg-gray-50 rounded px-3 py-2 flex items-center gap-2">
                <Info size={13} className="shrink-0" />
                Límite mensual Nequi en $0 — configure el valor en Configuración → General para activar el control del 4×1000.
              </div>
            )}

            {!conceptoFijo && (
              <>
                <div className="form-group">
                  <label className="form-label">Concepto (opcional)</label>
                  <input
                    className="form-control"
                    value={form.concepto}
                    onChange={(e) => set("concepto", e.target.value)}
                    placeholder={`Transferencia ${form.monedaOrigen} → ${form.monedaDestino}`}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">Observación (opcional)</label>
                  <input
                    className="form-control"
                    value={form.observacion}
                    onChange={(e) => set("observacion", e.target.value)}
                  />
                </div>
              </>
            )}
          </div>

          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancelar</button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? "Registrando…" : titulo ? `Registrar ${titulo.split(" ")[0].toLowerCase()}` : "Registrar transferencia"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
