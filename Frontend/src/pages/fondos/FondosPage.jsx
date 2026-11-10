import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Download, RotateCcw, TrendingUp, TrendingDown,
  DollarSign, SlidersHorizontal, Plus, ArrowUpRight, Minus, ChevronDown,
} from "lucide-react";
import { fondoService } from "../../services/fondoService";
import { gastoService } from "../../services/gastoService";
import { tasaService } from "../../services/tasaService";
import { exportacionService } from "../../services/exportacionService";
import { formatCOP, formatHora, formatUSD } from "../../utils/format";
import { TIPO_MOV, TIPO_MOV_LABEL } from "../../utils/movimientoTipos";
import Pagination from "../../components/shared/Pagination";
import ModalEntrada from "../../components/fondos/ModalEntrada";
import ModalTransferencia from "../../components/fondos/ModalTransferencia";
import ModalGasto from "../../components/gastos/ModalGasto";

/* ── helpers de formato ──────────────────────────────────────────── */
const sysTZ = () => { try { return Intl.DateTimeFormat().resolvedOptions().timeZone; } catch { return undefined; } };

const formatFechaFondos = (iso) => {
  if (!iso) return "—";
  const d = new Date(iso);
  const tz = sysTZ();
  const fecha = d.toLocaleDateString("es-CO", { day: "2-digit", month: "2-digit", year: "numeric", timeZone: tz });
  const hora  = d.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: tz });
  return `${fecha} · ${hora}`;
};

/** Formato compacto para la franja colapsada: $18.1M, $500K, $0 */
const formatSaldoCompacto = (saldo, moneda) => {
  if (moneda === "USD") return formatUSD(saldo ?? 0);
  const n = Math.abs(Number(saldo ?? 0));
  if (n >= 1_000_000) return `$${(Number(saldo ?? 0) / 1_000_000).toFixed(1)}M`;
  if (n >= 1_000)     return `$${(Number(saldo ?? 0) / 1_000).toFixed(0)}K`;
  return formatCOP(saldo ?? 0);
};

/* ── helpers visuales ─────────────────────────────────────────────── */
function getFondoLabel(fondo) {
  if (fondo.moneda === "COP")      return "Banco";
  if (fondo.moneda === "USD")      return "Dólares estadounidenses";
  if (fondo.moneda === "EFECTIVO") return "Efectivo (caja física)";
  if (fondo.moneda.startsWith("NEQUI"))
    return `Nequi${fondo.alias ? ` ${fondo.alias}` : ""}`;
  return fondo.moneda;
}

function getColorCls(moneda) {
  if (moneda === "COP") return "text-verde-claro";
  if (moneda === "USD") return "text-[#1a6b3c]";
  if (moneda === "EFECTIVO") return "text-amber-600";
  if (moneda.startsWith("NEQUI")) return "text-[#8c00ff]";
  return "text-verde-oscuro";
}

function getBorderCls(moneda) {
  if (moneda === "COP") return "border-l-verde-claro";
  if (moneda === "USD") return "border-l-[#1a6b3c]";
  if (moneda === "EFECTIVO") return "border-l-amber-500";
  if (moneda.startsWith("NEQUI")) return "border-l-[#8c00ff]";
  return "border-l-verde-claro";
}

function getMonedaBadge(moneda) {
  if (moneda === "COP") return { cls: "badge", style: { background: "#e8f4ec", color: "#2d6a4f" } };
  if (moneda === "USD") return { cls: "badge", style: { background: "#dbeafe", color: "#1e40af" } };
  if (moneda === "EFECTIVO") return { cls: "badge", style: { background: "#fef3c7", color: "#b45309" } };
  if (moneda.startsWith("NEQUI")) return { cls: "badge", style: { background: "#f0e6ff", color: "#8c00ff" } };
  return { cls: "badge badge-gray", style: {} };
}

/** Agrupa todos los movimientos tipo "cobro" por día + moneda en una única fila "Venta del día". */
function procesarHistorial(movs) {
  const cobros = movs.filter((m) => m.tipo === TIPO_MOV.COBRO);
  const otros  = movs.filter((m) => m.tipo !== TIPO_MOV.COBRO);

  const grupos = {};
  cobros.forEach((m) => {
    const dia = m.fecha.substring(0, 10);
    const key = `${dia}__${m.moneda}`;
    if (!grupos[key]) grupos[key] = { dia, moneda: m.moneda, monto: 0, montoCop: 0 };
    grupos[key].monto    += Number(m.monto    ?? 0);
    grupos[key].montoCop += Number(m.montoCop ?? 0);
  });

  const ventasDia = Object.values(grupos).map((g) => ({
    id:          `venta-${g.dia}-${g.moneda}`,
    tipo:        TIPO_MOV.COBRO,
    concepto:    "Venta del día",
    moneda:      g.moneda,
    monto:       g.monto,
    montoCop:    g.montoCop,
    fecha:       `${g.dia}T23:59:59`,
    _esVentaDia: true,
    _fechaNav:   g.dia,
  }));

  return [...ventasDia, ...otros].sort((a, b) => b.fecha.localeCompare(a.fecha));
}

const PAGE_SIZE = 25;

export default function FondosPage() {
  const navigate = useNavigate();
  const qc = useQueryClient();

  const [fondosExpandidos, setFondosExpandidos]         = useState(false);
  const [modalEntradaOpen, setModalEntradaOpen]         = useState(false);
  const [modalSalidaOpen,  setModalSalidaOpen]          = useState(false);
  const [modalTransferenciaMoneda, setModalTransferenciaMoneda] = useState(null);
  const [filtroFecha,  setFiltroFecha]  = useState("");
  const [filtroMoneda, setFiltroMoneda] = useState("");
  const [filtroTipo,   setFiltroTipo]   = useState("");
  const [pagina, setPagina] = useState(0);

  const { data: fondos = [], isLoading: loadingFondos } = useQuery({
    queryKey: ["fondos"],
    queryFn: fondoService.listar,
  });

  const { data: historial = [], isLoading: loadingHistorial } = useQuery({
    queryKey: ["fondos-historial", filtroFecha, filtroMoneda, filtroTipo],
    queryFn: () => fondoService.historial({
      ...(filtroFecha  ? { fecha: filtroFecha }   : {}),
      ...(filtroMoneda ? { moneda: filtroMoneda } : {}),
      ...(filtroTipo   ? { tipo: filtroTipo }     : {}),
    }),
    staleTime: 0,
  });

  const { data: tasa } = useQuery({
    queryKey: ["tasa-vigente"],
    queryFn: tasaService.vigente,
    retry: false,
  });

  const invalidarFondos = () => {
    qc.invalidateQueries({ queryKey: ["fondos"] });
    qc.invalidateQueries({ queryKey: ["fondos-historial"] });
  };

  const gastoMut = useMutation({
    mutationFn: gastoService.registrar,
    onSuccess: () => {
      invalidarFondos();
      qc.invalidateQueries({ queryKey: ["gastos"] });
    },
  });

  const entradaMut = useMutation({
    mutationFn: fondoService.registrarEntrada,
    onSuccess: invalidarFondos,
  });

  const transferenciaMut = useMutation({
    mutationFn: fondoService.registrarTransferencia,
    onSuccess: () => {
      invalidarFondos();
      qc.invalidateQueries({ queryKey: ["nequi-estado-mes"] });
    },
  });

  /* ── cálculos ─────────────────────────────────────────────────── */
  const nequiFondos = fondos.filter((f) => f.moneda.startsWith("NEQUI"));
  const tasaValor   = tasa?.valorCop ?? null;

  // Capital neto acumulado = suma de TODAS las billeteras del sistema
  // (Banco, Efectivo, Nequi, USD convertido a COP y cualquier fondo nuevo
  // que se agregue a futuro) — genérico sobre el array de fondos, no una
  // lista fija de monedas. USD se excluye del total mientras no haya tasa
  // vigente cargada (no se puede convertir con certeza).
  const capitalTotal = fondos.reduce((total, f) => {
    const saldo = Number(f.saldo ?? 0);
    if (f.moneda === "USD") {
      return tasaValor != null ? total + saldo * tasaValor : total;
    }
    return total + saldo;
  }, 0);

  const historialProcesado = useMemo(() => procesarHistorial(historial), [historial]);
  const totalPaginas  = Math.max(1, Math.ceil(historialProcesado.length / PAGE_SIZE));
  const paginaActual  = Math.min(pagina, totalPaginas - 1);
  const historialPag  = historialProcesado.slice(paginaActual * PAGE_SIZE, (paginaActual + 1) * PAGE_SIZE);

  const hayFiltro = filtroFecha || filtroMoneda || filtroTipo;
  const limpiarFiltros = () => { setFiltroFecha(""); setFiltroMoneda(""); setFiltroTipo(""); setPagina(0); };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Fondos</h1>
      </div>

      {/* Card unificada: capital total + fondos individuales */}
      {loadingFondos ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : (
        <div className="card mb-5 overflow-hidden border-l-4 border-l-verde-oscuro">
          {/* Header oscuro — toggle expand/collapse */}
          <div
            className="p-[18px] flex justify-between items-center flex-wrap gap-3 cursor-pointer select-none"
            style={{ background: "linear-gradient(135deg, #1a4731 0%, #1a4a2e 100%)" }}
            onClick={() => setFondosExpandidos((v) => !v)}
          >
            <div>
              <div className="flex items-center gap-2 mb-1">
                <DollarSign size={18} className="text-white/70" />
                <span className="text-[13px] text-white/75 font-medium">Capital neto acumulado</span>
              </div>
              <div className="text-[34px] font-black tracking-tight text-white">
                {formatCOP(capitalTotal)}
              </div>
            </div>
            <ChevronDown
              size={20}
              className="text-white/60 shrink-0 transition-transform duration-200"
              style={{ transform: fondosExpandidos ? "rotate(180deg)" : "rotate(0deg)" }}
            />
          </div>

          {/* Franja inferior — solo cuando está colapsado, muestra saldos compactos */}
          {!fondosExpandidos && (
            <div className="px-[18px] py-2 flex items-center gap-1.5 border-t border-gray-100 bg-white">
              <span className="text-[11px] text-verde-oscuro/60">
                {fondos.map((f) => `${f.moneda} ${formatSaldoCompacto(f.saldo, f.moneda)}`).join(" · ")}
              </span>
            </div>
          )}

          {/* Fondos individuales — animación max-height + opacity */}
          <div
            className="overflow-hidden transition-all duration-300 ease-in-out"
            style={{ maxHeight: fondosExpandidos ? "600px" : "0px", opacity: fondosExpandidos ? 1 : 0 }}
          >
            <div className="p-4 bg-white">
              <div className="grid gap-3" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))" }}>
                {fondos.map((f) => {
                  return (
                    <div key={f.moneda} className={`border-l-4 ${getBorderCls(f.moneda)} pl-4 py-3 flex flex-col`}>
                      <div className="text-[12px] text-gray-500 mb-1">{getFondoLabel(f)}</div>
                      <div className={`text-[22px] font-extrabold flex-1 ${(f.saldo ?? 0) < 0 ? "text-red-600" : getColorCls(f.moneda)}`}>
                        {f.moneda === "USD" ? formatUSD(f.saldo ?? 0) : formatCOP(f.saldo ?? 0)}
                      </div>
                      <div className="flex items-center justify-between gap-2 mt-2">
                        <div className="text-[11px] text-gray-500 flex flex-col gap-0.5">
                          <span>
                            {f.moneda}
                            {f.moneda === "USD" && tasaValor != null && (
                              <span> · ≈ {formatCOP(Number(f.saldo ?? 0) * tasaValor)} COP</span>
                            )}
                          </span>
                          {f.actualizadoEn && <span>act. {formatHora(f.actualizadoEn)}</span>}
                        </div>
                        <div className="h-[28px] flex items-center">
                          {/* Toda billetera puede transferir a cualquier otra — destino
                              por defecto Efectivo (Banco si el origen ya es Efectivo). */}
                          <button
                            className="btn btn-secondary btn-sm flex items-center gap-1"
                            onClick={(e) => { e.stopPropagation(); setModalTransferenciaMoneda(f.moneda); }}
                          >
                            <ArrowUpRight size={13} /> Transferir
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Libro de caja */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Libro de caja</span>
          <div className="flex items-center gap-2 ml-auto">
            <button className="btn btn-primary btn-sm flex items-center gap-1" onClick={() => setModalEntradaOpen(true)}>
              <Plus size={14} /> Nueva entrada
            </button>
            <button className="btn btn-danger btn-sm flex items-center gap-1" onClick={() => setModalSalidaOpen(true)}>
              <Minus size={14} /> Nueva salida
            </button>
            <a className="btn btn-secondary btn-sm flex items-center gap-1" href={exportacionService.cajaUrl(filtroFecha)}>
              <Download size={14} /> CSV
            </a>
          </div>
        </div>

        <div className="toolbar px-[18px] pt-4 mb-0">
          <div className="flex items-center gap-1.5 text-[13px] text-gray-500 mr-1">
            <SlidersHorizontal size={15} />
            <span>{historialProcesado.length} registros</span>
          </div>
          <input
            type="date"
            className="form-control w-40"
            value={filtroFecha}
            onChange={(e) => { setFiltroFecha(e.target.value); setPagina(0); }}
          />
          <select className="form-control w-36" value={filtroMoneda} onChange={(e) => { setFiltroMoneda(e.target.value); setPagina(0); }}>
            <option value="">Todas</option>
            <option value="COP">COP</option>
            <option value="USD">USD</option>
            <option value="EFECTIVO">Efectivo</option>
            {nequiFondos.map((f) => (
              <option key={f.moneda} value={f.moneda}>
                {f.alias ? `Nequi ${f.alias}` : f.moneda}
              </option>
            ))}
          </select>
          <select className="form-control w-36" value={filtroTipo} onChange={(e) => { setFiltroTipo(e.target.value); setPagina(0); }}>
            <option value="">Todo tipo</option>
            {Object.entries(TIPO_MOV_LABEL).map(([valor, label]) => (
              <option key={valor} value={valor}>{label}</option>
            ))}
          </select>
          <button
            className="btn btn-secondary btn-sm"
            style={{ visibility: hayFiltro ? "visible" : "hidden" }}
            onClick={limpiarFiltros}
          >
            Limpiar
          </button>
        </div>

        {/* Paginación en el header si hay más de una página */}
        {historialProcesado.length > PAGE_SIZE && (
          <div className="flex justify-center py-2 border-b border-gray-100">
            <Pagination
              pagina={paginaActual}
              totalItems={historialProcesado.length}
              pageSize={PAGE_SIZE}
              onChange={setPagina}
            />
          </div>
        )}

        <div className="table-wrap">
          {loadingHistorial ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : historialProcesado.length === 0 ? (
            <div className="empty-state">
              <p>
                {hayFiltro
                  ? `Sin movimientos${filtroFecha ? ` para el ${filtroFecha}` : ""}${filtroMoneda ? ` en ${filtroMoneda}` : ""}${filtroTipo ? ` de tipo ${filtroTipo}` : ""}.`
                  : "Sin movimientos registrados."}
              </p>
              {hayFiltro && (
                <button className="btn btn-secondary btn-sm mt-3" onClick={limpiarFiltros}>
                  Limpiar filtros
                </button>
              )}
            </div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha / Hora</th>
                  <th>Tipo</th>
                  <th>Concepto</th>
                  <th className="text-right">Monto</th>
                  <th>Moneda</th>
                </tr>
              </thead>
              <tbody>
                {historialPag.map((m) => {
                  const esVentaDia = !!m._esVentaDia;
                  const esEntrada  = esVentaDia || m.tipo === TIPO_MOV.COBRO || m.tipo === TIPO_MOV.ENTRADA;
                  const esSalida   = m.tipo === TIPO_MOV.SALIDA;
                  const esReverso  = m.tipo === TIPO_MOV.REVERSO;
                  const esCobro    = esEntrada || esReverso;
                  const Icon  = esEntrada ? TrendingUp : esSalida ? TrendingDown : esReverso ? RotateCcw : DollarSign;
                  const badge = esEntrada || esReverso ? "badge-success" : esSalida ? "badge-danger" : "badge-warning";
                  const color = esEntrada || esReverso ? "text-verde-claro" : esSalida ? "text-red-600" : "text-gray-600";
                  const nequiFondo  = fondos.find((f) => f.moneda === m.moneda);
                  const monedaLabel = nequiFondo?.alias && m.moneda.startsWith("NEQUI")
                    ? `Nequi ${nequiFondo.alias}`
                    : m.moneda;
                  const { cls: monedaCls, style: monedaStyle } = getMonedaBadge(m.moneda);
                  const labelTipo = m.tipo === TIPO_MOV.COBRO ? "Venta" : m.tipo ? m.tipo.charAt(0).toUpperCase() + m.tipo.slice(1) : "";
                  return (
                    <tr
                      key={m.id}
                      className={`transition-colors hover:bg-gray-50 ${esVentaDia ? "cursor-pointer" : ""} ${m.moneda === "USD" ? "group" : ""}`}
                      onClick={esVentaDia ? () => navigate("/resumen", { state: { fecha: m._fechaNav } }) : undefined}
                      title={esVentaDia ? "Ver resumen del día" : undefined}
                    >
                      <td className="text-[12px] text-gray-500 whitespace-nowrap">{formatFechaFondos(m.fecha)}</td>
                      <td>
                        <span className={`badge gap-1 ${badge}`}>
                          <Icon size={11} />{labelTipo}
                        </span>
                      </td>
                      <td>{m.concepto}</td>
                      <td className={`text-right font-semibold ${color}`}>
                        {m.moneda === "USD" && m.montoCop != null ? (
                          <>
                            <span className="group-hover:hidden">{esCobro ? "+" : "−"}{formatUSD(m.monto)}</span>
                            <span className="hidden group-hover:inline">{esCobro ? "+" : "−"}{formatCOP(m.montoCop)}</span>
                          </>
                        ) : (
                          `${esCobro ? "+" : "−"}${formatCOP(m.monto)}`
                        )}
                      </td>
                      <td>
                        {m.moneda === "USD" ? (
                          <>
                            <span className={`${monedaCls} group-hover:hidden`} style={monedaStyle}>{monedaLabel}</span>
                            <span className="badge hidden group-hover:inline" style={{ background: "#e8f4ec", color: "#2d6a4f" }}>COP</span>
                          </>
                        ) : (
                          <span className={monedaCls} style={monedaStyle}>{monedaLabel}</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {modalSalidaOpen && (
        <ModalGasto
          onClose={() => setModalSalidaOpen(false)}
          onGuardar={(data) => gastoMut.mutateAsync(data)}
        />
      )}

      {modalEntradaOpen && (
        <ModalEntrada
          onClose={() => setModalEntradaOpen(false)}
          onGuardar={(data) => entradaMut.mutateAsync(data)}
        />
      )}

      {modalTransferenciaMoneda && (
        <ModalTransferencia
          fondos={fondos}
          monedaOrigenInicial={modalTransferenciaMoneda}
          monedaDestinoDefault={modalTransferenciaMoneda === "EFECTIVO" ? "COP" : "EFECTIVO"}
          onClose={() => setModalTransferenciaMoneda(null)}
          onGuardar={(data) => transferenciaMut.mutateAsync(data)}
        />
      )}
    </div>
  );
}
