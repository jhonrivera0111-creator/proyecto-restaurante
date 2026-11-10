import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowRight, AlertTriangle, Calendar } from "lucide-react";
import { resumenService } from "../../services/resumenService";
import { pedidoService } from "../../services/pedidoService";
import { inventarioService } from "../../services/inventarioService";
import { formatCOP, formatDecimal, formatCreadoEn } from "../../utils/format";
import { calcularInsumos, formatInsumo, formatReservaDisplay, sortByReserva } from "../../utils/pedidoHelpers";
import KpiCard from "../../components/shared/KpiCard";
import Pagination from "../../components/shared/Pagination";

const PAGE_SIZE = 8;

function StockCard({ s, estimado = 0 }) {
  const texto   = formatInsumo(s.insumo, s.stockActual) ?? "0";
  const deficit = Math.max(0, estimado - s.stockActual);

  const umbral = s.alertaMinima ?? 5;
  const color =
    deficit > 0 || s.stockActual < umbral * 0.4
      ? "red-600"
      : s.stockActual < umbral
      ? "amber-500"
      : "verde-claro";
  const borderCls = `border-l-${color}`;
  const textCls = color === "amber-500" ? "text-amber-600" : `text-${color}`;
  const label = s.insumo.charAt(0).toUpperCase() + s.insumo.slice(1);

  return (
    <div className={`kpi-card ${borderCls}`}>
      <div className="kpi-label">Stock {label}</div>
      <div className={`font-bold text-[18px] leading-snug mt-1 ${textCls}`}>
        {texto}{deficit > 0 && " ⚠️"}
      </div>
      {estimado > 0 && (
        <div className="text-[12px] text-gray-500 mt-1.5 leading-snug">
          Pedidos: {formatInsumo(s.insumo, estimado)}
        </div>
      )}
      {deficit > 0 && (
        <div className="text-[12px] font-semibold text-red-600 mt-0.5 leading-snug">
          Falta: {formatInsumo(s.insumo, deficit)}
        </div>
      )}
    </div>
  );
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [hoy, setHoy] = useState(() => new Date().toLocaleDateString("en-CA"));
  const [soloReservas, setSoloReservas] = useState(false);
  const [pagina, setPagina] = useState(0);

  const { data: resumen } = useQuery({
    queryKey: ["resumen", hoy],
    queryFn: () => resumenService.obtener(hoy),
    staleTime: 15_000,
    refetchInterval: 30_000,
  });

  const { data: pedidosActivos = [] } = useQuery({
    queryKey: ["pedidos", false],
    queryFn: () => pedidoService.listar(false),
    staleTime: 10_000,
    refetchInterval: 20_000,
  });

  const { data: stock = [] } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn: inventarioService.listarStock,
    staleTime: 30_000,
    refetchInterval: 30_000,
  });

  // Actualiza "hoy" exactamente a medianoche para que el resumen cambie de día
  useEffect(() => {
    const now    = new Date();
    const mañana = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    const t = setTimeout(() => setHoy(new Date().toLocaleDateString("en-CA")), mañana.getTime() - now.getTime() + 100);
    return () => clearTimeout(t);
  }, [hoy]);


  const r = resumen;
  const porCobrar      = pedidosActivos.reduce((s, p) => s + (p.saldoPendiente ?? 0), 0);
  const insumosActivos = calcularInsumos(pedidosActivos.flatMap((p) => p.detalles ?? []));
  const reservasCount = pedidosActivos.filter((p) => !!(p.fechaReserva || p.horaReserva)).length;

  const insumosMap = useMemo(() => {
    const map = new Map();
    pedidosActivos.forEach((p) => map.set(p.id, calcularInsumos(p.detalles ?? [])));
    return map;
  }, [pedidosActivos]);

  const listaFiltrada = sortByReserva(
    soloReservas
      ? pedidosActivos.filter((p) => !!(p.fechaReserva || p.horaReserva))
      : pedidosActivos
  );

  const totalPaginas = Math.max(1, Math.ceil(listaFiltrada.length / PAGE_SIZE));
  const paginaActual = Math.min(pagina, totalPaginas - 1);
  const listaPagina = listaFiltrada.slice(paginaActual * PAGE_SIZE, (paginaActual + 1) * PAGE_SIZE);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <span className="text-[13px] text-gray-500">
          {new Date().toLocaleDateString("es-CO", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}
        </span>
      </div>

      {/* ---- Operaciones del día ---- */}
      <div className="font-semibold text-verde-oscuro mb-2 text-[13px]">Operaciones del día</div>
      <div className="kpi-grid mb-5">
        <KpiCard
          label="Pedidos pendientes"
          value={pedidosActivos.length}
          color={pedidosActivos.length > 0 ? "var(--warning)" : "var(--verde-claro)"}
          sub={pedidosActivos.length > 0 ? "pendientes ahora" : "ninguno pendiente"}
        />
        <KpiCard
          label="Por cobrar"
          value={formatCOP(porCobrar)}
          color={porCobrar > 0 ? "var(--warning)" : "var(--verde-claro)"}
        />
        <KpiCard
          label="Pedidos finalizados hoy"
          value={r?.pedidosFinalizados ?? "—"}
        />
        <KpiCard
          label="Cobrado hoy"
          value={r ? formatCOP(r.cobradoTotalCop) : "—"}
          color="var(--verde-claro)"
        />
        <KpiCard
          label="Gastos hoy"
          value={r ? formatCOP(r.gastosTotalCop) : "—"}
          color={r?.gastosTotalCop > 0 ? "var(--danger)" : undefined}
        />
      </div>

      {/* ---- Stock ---- */}
      {stock.length > 0 && (
        <>
          <div className="font-semibold text-verde-oscuro mb-2 text-[13px]">Inventario</div>
          <div className="kpi-grid mb-6">
            {stock.map((s) => <StockCard key={s.insumo} s={s} estimado={insumosActivos[s.insumo] ?? 0} />)}
          </div>
        </>
      )}

      {/* ---- Pedidos pendientes ---- */}
      <div className="card">
        <div className="card-header">
          <span className="card-title shrink-0">Pedidos pendientes</span>

          {/* Paginación centrada */}
          <div className="flex-1 flex justify-center">
            {listaFiltrada.length > PAGE_SIZE && (
              <Pagination pagina={paginaActual} totalItems={listaFiltrada.length} pageSize={PAGE_SIZE} onChange={setPagina} />
            )}
          </div>

          <div className="flex items-center gap-2 shrink-0">
            {reservasCount > 0 && (
              <button
                className={`btn btn-sm flex items-center gap-1.5 ${soloReservas ? "btn-primary" : "btn-secondary"}`}
                onClick={() => { setSoloReservas((v) => !v); setPagina(0); }}
                title="Filtrar por reservas"
              >
                <Calendar size={13} />
                Reservas
                <span className={`rounded-full px-1.5 py-0 text-[10px] font-bold leading-4 ${soloReservas ? "bg-white/20 text-white" : "bg-amber-100 text-amber-700"}`}>
                  {reservasCount}
                </span>
              </button>
            )}
            <button
              className="btn btn-secondary btn-sm flex items-center gap-1"
              onClick={() => navigate("/pedidos")}
            >
              Ver todos <ArrowRight size={13} />
            </button>
          </div>
        </div>
        <div className="table-wrap">
          {pedidosActivos.length === 0 ? (
            <div className="empty-state"><p>No hay pedidos pendientes en este momento</p></div>
          ) : listaFiltrada.length === 0 ? (
            <div className="empty-state"><p>No hay reservas activas</p></div>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Cliente</th>
                    <th>Productos</th>
                    <th className="text-right">Total</th>
                    <th className="text-right">Saldo</th>
                    <th>Fecha/Hora</th>
                  </tr>
                </thead>
                <tbody>
                  {listaPagina.map((p) => {
                    const insumos = insumosMap.get(p.id) ?? {};
                    const gallina = insumos["gallina"];
                    const cuy = insumos["cuy"];
                    const tieneReserva = !!(p.fechaReserva || p.horaReserva);
                    return (
                    <tr
                      key={p.id}
                      className="cursor-pointer"
                      onClick={() => navigate("/pedidos", { state: { abrirPedidoId: p.id } })}
                      title="Ver pedido"
                    >
                      <td className="font-bold text-gray-500 text-[12px]">#{p.id}</td>
                      <td className="font-semibold">
                        <span>{p.nombre ?? "—"}</span>
                        {tieneReserva && (
                          <Calendar size={11} className="inline ml-1.5 text-amber-500" />
                        )}
                      </td>
                      <td>
                        {(gallina || cuy) ? (
                          <div className="flex gap-2 flex-wrap text-[12px] font-semibold text-gray-700">
                            {gallina && <span>🐔 {formatInsumo("gallina", gallina)}</span>}
                            {cuy && <span>🐹 {formatInsumo("cuy", cuy)}</span>}
                          </div>
                        ) : (
                          <span className="text-[12px] text-gray-500">
                            {p.detalles?.slice(0, 2).map((d) => d.nombreSnapshot ?? d.nombre).filter(Boolean).join(", ") || "—"}
                          </span>
                        )}
                      </td>
                      <td className="text-right font-semibold">{formatCOP(p.totalCop)}</td>
                      <td className={`text-right font-bold ${p.saldoPendiente > 0 ? "text-red-600" : "text-verde-claro"}`}>
                        {p.saldoPendiente > 0 ? formatCOP(p.saldoPendiente) : "Pagado"}
                      </td>
                      <td className="text-[12px]">
                        {tieneReserva
                          ? <span className="text-amber-600">{formatReservaDisplay(p.fechaReserva, p.horaReserva)}</span>
                          : <span className="text-gray-500">{formatCreadoEn(p.creadoEn)}</span>
                        }
                      </td>
                    </tr>
                    );
                  })}
                </tbody>
              </table>
            </>
          )}
        </div>
      </div>

      {/* Alerta de stock bajo */}
      {stock.some((s) => s.stockActual < (s.alertaMinima ?? 5) * 0.4) && (
        <div className="mt-3.5 px-[14px] py-[10px] rounded-lg bg-red-600/[0.08] border border-red-600 flex items-center gap-2.5 text-[13px] text-red-600">
          <AlertTriangle size={16} />
          <span>
            <strong>Stock crítico:</strong>{" "}
            {stock.filter((s) => s.stockActual < (s.alertaMinima ?? 5) * 0.4).map((s) => `${s.insumo} (${formatDecimal(s.stockActual)})`).join(", ")}
          </span>
        </div>
      )}
    </div>
  );
}
