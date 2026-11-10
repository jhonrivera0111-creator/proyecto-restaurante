import { useState, useEffect, useRef, useMemo, useCallback } from "react";
import { useLocation } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Plus, Search, X, CheckCircle, ChevronDown, ChevronUp, Calendar, ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { pedidoService } from "../../services/pedidoService";
import { productoService } from "../../services/productoService";
import { salonService } from "../../services/salonService";
import { inventarioService } from "../../services/inventarioService";
import { formatCOP } from "../../utils/format";
import { esActivo, sortByReserva, calcularInsumos, formatInsumo } from "../../utils/pedidoHelpers";
import { usePedidoMutations } from "../../hooks/usePedidoMutations";
import { useSessionStore } from "../../store/useSessionStore";
import OrdenCard from "../../components/pedidos/OrdenCard";
import ModalCrearPedido from "../../components/pedidos/ModalCrearPedido";
import ModalDetallePedido from "../../components/pedidos/ModalDetallePedido";
import ModalCobrar from "../../components/pedidos/ModalCobrar";
import ModalPedidoHistorico from "../../components/pedidos/ModalPedidoHistorico";
import Pagination from "../../components/shared/Pagination";

const CARD_MIN = 280; // minmax(280px, ...) del grid
const CARD_GAP = 14;  // gap-[14px]
const ROWS     = 2;   // filas por página
const getHoy = () => new Date().toLocaleDateString("en-CA");

// Fecha "efectiva" de un pedido para ordenar/filtrar por día: la reserva si
// tiene, si no cuándo se creó (mismo criterio que sortByReserva).
const fechaEfectiva = (p) => p.fechaReserva ?? (p.creadoEn ?? "").slice(0, 10);

/** Ordena por fecha efectiva (reserva u hora, si no creadoEn). asc | desc. */
function ordenarPorFecha(list, dir) {
  const ts = (p) => p.fechaReserva
    ? new Date(`${p.fechaReserva}T${p.horaReserva ?? "00:00"}`).getTime()
    : new Date(p.creadoEn).getTime();
  return [...list].sort((a, b) => (dir === "asc" ? ts(a) - ts(b) : ts(b) - ts(a)));
}

/** Botón de orden asc/desc. Para Activos incluye un tercer estado "auto"
 * (null) que usa el orden inteligente por reserva — el explícito asc/desc
 * lo reemplaza por orden cronológico simple. */
function OrdenToggle({ modo, onChange, conAuto }) {
  const ciclo = conAuto
    ? { null: "asc", asc: "desc", desc: null }
    : { desc: "asc", asc: "desc" };
  const siguiente = ciclo[modo === null ? "null" : modo] ?? (conAuto ? null : "desc");
  const label = modo === "asc" ? "Más antiguos primero" : modo === "desc" ? "Más recientes primero" : "Orden automático (reservas)";
  const Icon = modo === "asc" ? ArrowUp : modo === "desc" ? ArrowDown : ArrowUpDown;
  return (
    <button
      type="button"
      className="btn btn-secondary btn-sm px-2"
      title={label}
      onClick={() => onChange(siguiente)}
    >
      <Icon size={13} />
    </button>
  );
}

// ── Overlay de éxito después de crear pedido ─────────────────────────────────
function PedidoCreado({ pedido, onVerPedido, onCerrar }) {
  useModalKeyboard({ onEscape: onCerrar });
  const totalItems = pedido.detalles?.length ?? 0;
  return (
    <div className="fixed inset-0 bg-black/50 z-[1000] flex items-center justify-center p-4 animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden animate-slide-up">
        <div className="h-1.5 bg-verde-claro w-full" />
        <div className="p-8 text-center">
          <div className="w-16 h-16 rounded-full bg-verde-claro/10 border-2 border-verde-claro/20 flex items-center justify-center mx-auto mb-4">
            <CheckCircle size={36} className="text-verde-claro" />
          </div>
          <div className="text-[11px] font-bold text-gray-400 uppercase tracking-widest mb-1">
            Pedido registrado
          </div>
          <h2 className="text-2xl font-black text-gray-900 mb-1">
            {pedido.nombre ?? `Pedido #${pedido.id}`}
          </h2>
          <div className="text-sm text-gray-400 mb-3">
            #{pedido.id}
            {totalItems > 0 && ` · ${totalItems} ítem${totalItems !== 1 ? "s" : ""}`}
          </div>
          <div className="text-3xl font-black text-verde-oscuro mb-2">
            {formatCOP(pedido.totalCop)}
          </div>
          {pedido.totalAbonado > 0 && (
            <div className="text-sm text-verde-claro font-semibold mb-1">
              Abono registrado: {formatCOP(pedido.totalAbonado)}
            </div>
          )}
          {pedido.saldoPendiente > 0 && (
            <div className="text-sm text-red-500 font-semibold mb-1">
              Saldo pendiente: {formatCOP(pedido.saldoPendiente)}
            </div>
          )}
          <div className="flex flex-col gap-2.5 mt-6">
            <button className="btn btn-primary w-full justify-center py-2.5 text-[15px]" onClick={onVerPedido}>
              Ver pedido
            </button>
            <button
              className="btn w-full justify-center border border-verde-claro text-verde-claro hover:bg-verde-bg py-2.5 text-[15px]"
              onClick={onCerrar}
            >
              Listo, cerrar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Página principal ──────────────────────────────────────────────────────────
export default function PedidosPage() {
  const location = useLocation();
  const usuario = useSessionStore((s) => s.usuario);
  const [pendienteAbrirId, setPendienteAbrirId] = useState(() => location.state?.abrirPedidoId ?? null);
  const [showCrear, setShowCrear]                 = useState(false);
  const [showHistorico, setShowHistorico]         = useState(false);
  const [pedidoActivo, setPedidoActivo]           = useState(null);
  const [pedidoCobrar, setPedidoCobrar]           = useState(null);
  const [pedidoCreadoExito, setPedidoCreadoExito] = useState(null);
  const [tab, setTab]                             = useState(() => location.state?.irAHistorial ? "historial" : "activos");
  const [busqueda, setBusqueda]                   = useState("");
  const [kpiFiltro, setKpiFiltro]                 = useState(null); // null | "en-curso" | "reservas" | "para-llevar"
  const [pagina, setPagina]                       = useState(0);
  const [fechaHistorial, setFechaHistorial]       = useState("");
  const [fechaActivos, setFechaActivos]           = useState("");
  const [ordenActivos, setOrdenActivos]           = useState(null); // null = automático (sortByReserva)
  const [ordenHistorial, setOrdenHistorial]       = useState("desc");
  const [kpiVisible, setKpiVisible]               = useState(true);
  const [pageSize, setPageSize]                   = useState(CARD_MIN * ROWS); // inicial conservador
  const gridRef                                   = useRef(null);

  // Calcula cuántas tarjetas caben en ROWS filas según el ancho real del grid
  useEffect(() => {
    if (!gridRef.current) return;
    const observer = new ResizeObserver(([entry]) => {
      const w = entry.contentRect.width;
      const cols = Math.max(1, Math.floor((w + CARD_GAP) / (CARD_MIN + CARD_GAP)));
      setPageSize(cols * ROWS);
    });
    observer.observe(gridRef.current);
    return () => observer.disconnect();
  }, []);


  const verTodos = tab === "historial";

  const { data: pedidos = [], isLoading } = useQuery({
    queryKey: ["pedidos", verTodos],
    queryFn: () => pedidoService.listar(verTodos),
    refetchInterval: 15_000,
  });

  const { data: productos = [] } = useQuery({
    queryKey: ["productos-activos"],
    queryFn: () => productoService.listar(true),
  });

  const { data: salones = [] } = useQuery({
    queryKey: ["salones"],
    queryFn: salonService.listar,
  });

  const { data: stockData = [] } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn: inventarioService.listarStock,
    staleTime: 30_000,
    refetchInterval: 60_000,
  });

  // Expande la tarjeta del pedido indicado al navegar desde Dashboard o Resumen
  const [pedidoExpandidoId, setPedidoExpandidoId] = useState(null);
  useEffect(() => {
    if (!pendienteAbrirId || pedidos.length === 0) return;
    // Esperar a que ResizeObserver actualice pageSize desde su valor inicial (CARD_MIN * ROWS)
    if (pageSize === CARD_MIN * ROWS) return;
    const pedido = pedidos.find((p) => p.id === pendienteAbrirId);
    if (!pedido) return;
    setPedidoExpandidoId(pedido.id);
    // Navegar a la página que contiene el pedido
    if (esActivo(pedido.estado)) {
      // Activos: misma lista y orden que usa la paginación (sortByReserva, sin filtro)
      const lista = sortByReserva(pedidos.filter((p) => esActivo(p.estado)));
      const idx   = lista.findIndex((p) => p.id === pendienteAbrirId);
      if (idx >= 0) setPagina(Math.floor(idx / pageSize));
    } else {
      // Historial: pedidos FINALIZADO + ANULADO en el orden que llegan del backend
      const hist = pedidos.filter((p) => p.estado === "FINALIZADO" || p.estado === "ANULADO");
      const idx  = hist.findIndex((p) => p.id === pendienteAbrirId);
      if (idx >= 0) setPagina(Math.floor(idx / pageSize));
    }
    setPendienteAbrirId(null);
    window.history.replaceState({}, "");
  }, [pendienteAbrirId, pedidos, pageSize]);


  const {
    crearMut, agregarMut, actualizarDetalleMut, quitarDetalleMut,
    actualizarMut, pagoMut, actualizarPagoMut, quitarPagoMut,
    finalizarMut, anularMut, fiarMut, crearHistoricoMut,
  } = usePedidoMutations();

  // ── Listas derivadas ──────────────────────────────────────────────────────
  const abiertos       = useMemo(() => pedidos.filter((p) => esActivo(p.estado)), [pedidos]);
  const enCursoList    = useMemo(() => abiertos.filter((p) => p.enCurso), [abiertos]);
  const reservasList   = useMemo(() => abiertos.filter((p) => !!(p.fechaReserva || p.horaReserva)), [abiertos]);
  const paraLlevarList = useMemo(() => abiertos.filter((p) => !!p.paraLlevar), [abiertos]);

  // Insumos totales necesarios para todos los pedidos activos
  const insumosTotal  = useMemo(() => calcularInsumos(abiertos.flatMap((p) => p.detalles ?? [])), [abiertos]);
  const totalGallina  = insumosTotal["gallina"] ?? 0;
  const totalCuy      = insumosTotal["cuy"]     ?? 0;

  // Stock actual y déficit
  const stockMap     = Object.fromEntries(stockData.map((s) => [s.insumo, s.stockActual]));
  const stockGallina = stockMap["gallina"] ?? 0;
  const stockCuy     = stockMap["cuy"]     ?? 0;
  const defGallina   = Math.max(0, totalGallina - stockGallina);
  const defCuy       = Math.max(0, totalCuy     - stockCuy);

  const historial = useMemo(() => pedidos.filter((p) => p.estado === "FINALIZADO" || p.estado === "ANULADO"), [pedidos]);
  const historialFiltrado = useMemo(() => fechaHistorial
    ? historial.filter((p) => (p.creadoEn ?? "").slice(0, 10) === fechaHistorial)
    : historial,
  [historial, fechaHistorial]);

  const handleEditar = useCallback((p) => setPedidoActivo(p), []);
  const handleCobrar = useCallback((p) => setPedidoCobrar(p), []);

  const toggleKpi = useCallback((filtro) => {
    setKpiFiltro((prev) => (prev === filtro ? null : filtro));
    setPagina(0);
  }, []);

  const kpiFiltroEfectivo =
    (kpiFiltro === "en-curso"    && enCursoList.length    > 0) ? "en-curso"    :
    (kpiFiltro === "reservas"    && reservasList.length   > 0) ? "reservas"    :
    (kpiFiltro === "para-llevar" && paraLlevarList.length > 0) ? "para-llevar" :
    null;

  const kpiLabel = {
    "en-curso":    "En curso",
    "reservas":    "Reservas",
    "para-llevar": "Para llevar",
  }[kpiFiltroEfectivo] ?? "Pendientes";

  const listaActivos =
    kpiFiltroEfectivo === "en-curso"    ? enCursoList    :
    kpiFiltroEfectivo === "reservas"    ? reservasList   :
    kpiFiltroEfectivo === "para-llevar" ? paraLlevarList :
    abiertos;

  const activosFiltradosPorFecha = useMemo(() => fechaActivos
    ? listaActivos.filter((p) => fechaEfectiva(p) === fechaActivos)
    : listaActivos,
  [listaActivos, fechaActivos]);

  const listaBase     = tab === "historial" ? historialFiltrado : activosFiltradosPorFecha;
  const listaFiltrada = useMemo(() => busqueda
    ? listaBase.filter((p) => (p.nombre ?? "").toLowerCase().includes(busqueda.toLowerCase()))
    : listaBase,
  [listaBase, busqueda]);
  const pedidosMostrados = useMemo(() => {
    if (tab === "activos") return ordenActivos ? ordenarPorFecha(listaFiltrada, ordenActivos) : sortByReserva(listaFiltrada);
    return ordenarPorFecha(listaFiltrada, ordenHistorial);
  }, [tab, listaFiltrada, ordenActivos, ordenHistorial]);

  // ── Paginación ────────────────────────────────────────────────────────────
  const totalPaginas = Math.max(1, Math.ceil(pedidosMostrados.length / pageSize));
  const paginaActual = Math.min(pagina, totalPaginas - 1);
  const pedidosPagina = pedidosMostrados.slice(paginaActual * pageSize, (paginaActual + 1) * pageSize);

  return (
    <div ref={gridRef} className="-mt-6">

      {/* ══ Bloque sticky: header + KPIs + tabs ═══════════════════════════ */}
      <div className="sticky -top-6 z-20 bg-page -mx-6 px-6 shadow-[0_4px_12px_-2px_rgba(0,0,0,0.08)]">

        {/* Fila 1: título + mini-resumen + botón */}
        <div className="flex items-center justify-between pt-2 pb-1.5">
          {/* Lado izquierdo: título + badges inline (cuando KPIs colapsados) */}
          <div className="flex items-center gap-2 flex-1 min-w-0 overflow-hidden">
            {tab === "historial" ? (
              <h1 className="page-title shrink-0">Pedidos</h1>
            ) : (
              <button
                className="flex items-center gap-1.5 group shrink-0"
                onClick={() => setKpiVisible(v => !v)}
                title={kpiVisible ? "Ocultar resumen" : "Mostrar resumen"}
              >
                <h1 className="page-title group-hover:text-verde-claro transition-colors">Pedidos</h1>
                {kpiVisible
                  ? <ChevronUp size={15} className="text-gray-400 group-hover:text-verde-claro transition-colors" />
                  : <ChevronDown size={15} className="text-gray-400 group-hover:text-verde-claro transition-colors" />
                }
              </button>
            )}

            {/* Mini KPI inline — aparece cuando KPIs están colapsados */}
            {tab === "activos" && (
              <div
                className={`flex items-center gap-2.5 overflow-hidden transition-[max-width,opacity] duration-350 ease-in-out ${
                  !kpiVisible ? "max-w-[420px] opacity-100" : "max-w-0 opacity-0 pointer-events-none"
                }`}
              >
                <span className="text-gray-300 shrink-0 select-none">|</span>
                <button
                  onClick={() => { setKpiFiltro(null); setPagina(0); }}
                  className={`shrink-0 whitespace-nowrap text-[12px] rounded px-1.5 py-0.5 transition-colors cursor-pointer select-none
                    ${!kpiFiltro ? "bg-gray-200 text-gray-800 font-bold ring-1 ring-gray-400/50" : "text-gray-500 hover:bg-gray-100"}`}
                >
                  <span className="font-bold tabular-nums">{abiertos.length}</span>
                  <span className="ml-1 font-medium">todos</span>
                </button>
                {enCursoList.length > 0 && (
                  <button
                    onClick={() => toggleKpi("en-curso")}
                    className={`shrink-0 whitespace-nowrap text-[12px] rounded px-1.5 py-0.5 transition-colors cursor-pointer select-none
                      ${kpiFiltro === "en-curso" ? "bg-green-50 text-green-700 font-bold ring-1 ring-green-400/40" : "text-green-600 hover:bg-green-50"}`}
                  >
                    <span className="font-bold tabular-nums">{enCursoList.length}</span>
                    <span className="ml-1 font-medium">en curso</span>
                  </button>
                )}
                {reservasList.length > 0 && (
                  <button
                    onClick={() => toggleKpi("reservas")}
                    className={`shrink-0 whitespace-nowrap text-[12px] rounded px-1.5 py-0.5 transition-colors cursor-pointer select-none
                      ${kpiFiltro === "reservas" ? "bg-amber-50 text-amber-700 font-bold ring-1 ring-amber-400/40" : "text-amber-500 hover:bg-amber-50"}`}
                  >
                    <span className="font-bold tabular-nums">{reservasList.length}</span>
                    <span className="ml-1 font-medium">reservas</span>
                  </button>
                )}
                {paraLlevarList.length > 0 && (
                  <button
                    onClick={() => toggleKpi("para-llevar")}
                    className={`shrink-0 whitespace-nowrap text-[12px] rounded px-1.5 py-0.5 transition-colors cursor-pointer select-none
                      ${kpiFiltro === "para-llevar" ? "bg-blue-50 text-blue-700 font-bold ring-1 ring-blue-400/40" : "text-blue-500 hover:bg-blue-50"}`}
                  >
                    <span className="font-bold tabular-nums">{paraLlevarList.length}</span>
                    <span className="ml-1 font-medium">para llevar</span>
                  </button>
                )}
              </div>
            )}
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {usuario?.rol === "ADMIN" && (
              <button
                className="btn btn-secondary text-amber-700 border-amber-300 hover:bg-amber-50"
                title="Carga histórica: crea, cobra y finaliza un pedido retroactivo (fondos e inventario reales)"
                onClick={() => setShowHistorico(true)}
              >
                📋 Carga histórica
              </button>
            )}
            <button className="btn btn-primary" onClick={() => setShowCrear(true)}>
              <Plus size={15} /> Nuevo pedido
            </button>
          </div>
        </div>

        {/* Fila 2: KPIs con animación de entrada/salida */}
        <div
          className={`overflow-hidden transition-[max-height,opacity] duration-350 ease-in-out ${
            tab === "activos" && kpiVisible
              ? "max-h-[160px] opacity-100"
              : "max-h-0 opacity-0"
          }`}
        >
          <div className="kpi-grid !mb-2">
            <div
              className={`kpi-card cursor-pointer transition-all hover:shadow-md select-none
                ${!kpiFiltro ? "border-l-gray-500 ring-2 ring-gray-400/40 bg-gray-50" : "border-l-gray-200"}`}
              onClick={() => { setKpiFiltro(null); setPagina(0); }}
            >
              <div className="kpi-label">Todos</div>
              <div className={`kpi-value ${!kpiFiltro ? "text-gray-800" : ""}`}>{abiertos.length}</div>
            </div>

            {enCursoList.length > 0 && (
              <div
                className={`kpi-card cursor-pointer transition-all hover:shadow-md select-none
                  ${kpiFiltro === "en-curso" ? "border-l-green-600 ring-2 ring-green-400/40 bg-green-50" : "border-l-green-400"}`}
                onClick={() => toggleKpi("en-curso")}
              >
                <div className="kpi-label">En curso</div>
                <div className="kpi-value text-green-600">{enCursoList.length}</div>
              </div>
            )}

            {reservasList.length > 0 && (
              <div
                className={`kpi-card cursor-pointer transition-all hover:shadow-md select-none
                  ${kpiFiltro === "reservas" ? "border-l-amber-600 ring-2 ring-amber-400/40 bg-amber-50" : "border-l-amber-400"}`}
                onClick={() => toggleKpi("reservas")}
              >
                <div className="kpi-label">Reservas</div>
                <div className="kpi-value text-amber-600">{reservasList.length}</div>
              </div>
            )}

            {paraLlevarList.length > 0 && (
              <div
                className={`kpi-card cursor-pointer transition-all hover:shadow-md select-none
                  ${kpiFiltro === "para-llevar" ? "border-l-blue-600 ring-2 ring-blue-400/40 bg-blue-50" : "border-l-blue-400"}`}
                onClick={() => toggleKpi("para-llevar")}
              >
                <div className="kpi-label">Para llevar</div>
                <div className="kpi-value text-blue-600">{paraLlevarList.length}</div>
              </div>
            )}
          </div>

        </div>

        {/* Fila 3: tabs + paginación + filtros */}
        <div className="flex items-center gap-3">
          <div className="tabs shrink-0 !mb-0">
            <button
              className={`tab${tab === "activos" ? " activo" : ""}`}
              onClick={() => { setTab("activos"); setPagina(0); }}
            >
              {kpiLabel}
              {listaActivos.length > 0 && (
                <span className="ml-1.5 bg-verde-claro text-white rounded-[10px] px-[7px] py-0.5 text-[11px] font-bold">
                  {listaActivos.length}
                </span>
              )}
            </button>
            <button
              className={`tab${tab === "historial" ? " activo" : ""}`}
              onClick={() => { setTab("historial"); setPagina(0); }}
            >
              Historial
            </button>
          </div>

          <div className="flex-1 flex justify-center">
            {totalPaginas > 1 && (
              <Pagination pagina={paginaActual} totalItems={listaFiltrada.length} pageSize={pageSize} onChange={setPagina} />
            )}
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {tab === "historial" && (
              <div className="flex items-center gap-1.5">
                <div className="relative flex items-center">
                  <Calendar size={13} className="absolute left-2.5 text-gray-400 pointer-events-none" />
                  <input
                    type="date"
                    className="form-control pl-8 pr-2 text-sm w-[148px]"
                    value={fechaHistorial}
                    onChange={(e) => { setFechaHistorial(e.target.value); setPagina(0); }}
                  />
                </div>
                <button
                  className={`btn btn-sm ${fechaHistorial === getHoy() ? "btn-primary" : "btn-secondary"}`}
                  onClick={() => { setFechaHistorial(getHoy()); setPagina(0); }}
                >
                  Hoy
                </button>
                {fechaHistorial && (
                  <button
                    className="btn btn-secondary btn-sm px-2"
                    title="Quitar filtro de fecha"
                    onClick={() => { setFechaHistorial(""); setPagina(0); }}
                  >
                    <X size={13} />
                  </button>
                )}
                <OrdenToggle modo={ordenHistorial} onChange={(m) => { setOrdenHistorial(m ?? "desc"); setPagina(0); }} conAuto={false} />
              </div>
            )}
            {tab === "activos" && (
              <div className="flex items-center gap-1.5">
                <div className="relative flex items-center">
                  <Calendar size={13} className="absolute left-2.5 text-gray-400 pointer-events-none" />
                  <input
                    type="date"
                    className="form-control pl-8 pr-2 text-sm w-[148px]"
                    value={fechaActivos}
                    onChange={(e) => { setFechaActivos(e.target.value); setPagina(0); }}
                  />
                </div>
                {fechaActivos && (
                  <button
                    className="btn btn-secondary btn-sm px-2"
                    title="Quitar filtro de fecha"
                    onClick={() => { setFechaActivos(""); setPagina(0); }}
                  >
                    <X size={13} />
                  </button>
                )}
                <OrdenToggle modo={ordenActivos} onChange={(m) => { setOrdenActivos(m); setPagina(0); }} conAuto={true} />
              </div>
            )}
            <div className="relative w-48">
              <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
              <input
                className="form-control pl-8 pr-8 text-sm"
                placeholder="Buscar cliente…"
                value={busqueda}
                onChange={(e) => { setBusqueda(e.target.value); setPagina(0); }}
              />
              {busqueda && (
                <button
                  className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                  onClick={() => { setBusqueda(""); setPagina(0); }}
                >
                  <X size={13} />
                </button>
              )}
            </div>
          </div>
        </div>

      {/* Franja de insumos estimados — siempre visible, solo en pestaña activos */}
      {tab === "activos" && (totalGallina > 0 || totalCuy > 0) && (
        <div className="flex items-center gap-x-5 gap-y-1 flex-wrap px-1 pt-2 pb-1.5 mt-1 border-t border-gray-200/80">
          <span className="text-[11px] font-semibold text-gray-400 uppercase tracking-wide shrink-0">
            Estimado pendiente
          </span>
          {totalGallina > 0 && (
            <span className={`text-[13px] font-bold ${defGallina > 0 ? "text-red-600" : "text-gray-800"}`}>
              🐔 {formatInsumo("gallina", totalGallina)}{defGallina > 0 && " ⚠️"}
            </span>
          )}
          {totalCuy > 0 && (
            <span className={`text-[13px] font-bold ${defCuy > 0 ? "text-red-600" : "text-gray-800"}`}>
              🐹 {formatInsumo("cuy", totalCuy)}{defCuy > 0 && " ⚠️"}
            </span>
          )}
        </div>
      )}

      </div>{/* fin sticky */}

      {/* ── Tarjetas (scroll de página) ────────────────────────────────── */}
      <div className="pt-2">
        {isLoading ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : pedidosMostrados.length === 0 ? (
          <div className="empty-state">
            <p>
              {busqueda
                ? `Sin resultados para "${busqueda}"`
                : tab === "historial" && fechaHistorial
                  ? `No hay pedidos en el historial para ${new Date(fechaHistorial + "T12:00:00").toLocaleDateString("es-CO", { day: "numeric", month: "long", year: "numeric" })}`
                  : tab === "activos"
                    ? "No hay pedidos abiertos. Crea el primero."
                    : "No hay pedidos en el historial."
              }
            </p>
          </div>
        ) : (
          <div className="grid gap-[14px]" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))" }}>
            {pedidosPagina.map((p, idx) => (
              <OrdenCard key={p.id} pedido={p} idx={idx}
                expandidoInicial={p.id === pedidoExpandidoId}
                onEditar={handleEditar}
                onCobrar={handleCobrar}
              />
            ))}
          </div>
        )}
      </div>

      {showCrear && (
        <ModalCrearPedido
          salones={salones}
          productos={productos}
          onClose={() => setShowCrear(false)}
          onCrear={(req) => crearMut.mutateAsync(req).then((res) => {
            setShowCrear(false);
            setPedidoCreadoExito(res);
          })}
        />
      )}

      {showHistorico && (
        <ModalPedidoHistorico
          productos={productos}
          onClose={() => setShowHistorico(false)}
          onCrear={(req) => crearHistoricoMut.mutateAsync(req)}
        />
      )}

      {pedidoActivo && (
        <ModalDetallePedido
          pedido={pedidoActivo}
          productos={productos}
          salones={salones}
          onClose={() => setPedidoActivo(null)}
          onActualizar={(req) => actualizarMut.mutateAsync({ id: pedidoActivo.id, req }).then(setPedidoActivo)}
          onAgregarDetalle={(req) => agregarMut.mutateAsync({ id: pedidoActivo.id, req }).then(setPedidoActivo)}
          onActualizarDetalle={(detalleId, req) => actualizarDetalleMut.mutateAsync({ id: pedidoActivo.id, detalleId, req }).then(setPedidoActivo)}
          onQuitarDetalle={(detalleId) => quitarDetalleMut.mutateAsync({ id: pedidoActivo.id, detalleId }).then(setPedidoActivo)}
          onFinalizar={() => finalizarMut.mutateAsync(pedidoActivo.id).then(setPedidoActivo)}
          onAnular={() => anularMut.mutateAsync(pedidoActivo.id).then(() => setPedidoActivo(null))}
          onFiar={() => fiarMut.mutateAsync(pedidoActivo.id).then(() => setPedidoActivo(null))}
          onActualizarPago={(pagoId, req) => actualizarPagoMut.mutateAsync({ id: pedidoActivo.id, pagoId, req }).then(setPedidoActivo)}
          onQuitarPago={(pagoId) => quitarPagoMut.mutateAsync({ id: pedidoActivo.id, pagoId }).then(setPedidoActivo)}
        />
      )}

      {pedidoCobrar && (
        <ModalCobrar
          pedido={pedidoCobrar}
          onClose={() => setPedidoCobrar(null)}
          onPago={(req) => pagoMut.mutateAsync({ id: pedidoCobrar.id, req }).then(setPedidoCobrar)}
          onActualizar={(req) => actualizarMut.mutateAsync({ id: pedidoCobrar.id, req }).then(setPedidoCobrar)}
          onFinalizar={() => finalizarMut.mutateAsync(pedidoCobrar.id).then(setPedidoCobrar)}
          onFiar={() => fiarMut.mutateAsync(pedidoCobrar.id).then(() => setPedidoCobrar(null))}
        />
      )}

      {pedidoCreadoExito && (
        <PedidoCreado
          pedido={pedidoCreadoExito}
          onVerPedido={() => { setPedidoActivo(pedidoCreadoExito); setPedidoCreadoExito(null); }}
          onCerrar={() => setPedidoCreadoExito(null)}
        />
      )}
    </div>
  );
}
