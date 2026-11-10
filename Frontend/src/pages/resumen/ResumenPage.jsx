import { useState, useEffect, useMemo } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { TrendingUp, TrendingDown, DollarSign, ShoppingBag, Download } from "lucide-react";
import { resumenService } from "../../services/resumenService";
import { cierreCajaService } from "../../services/cierreCajaService";
import { exportacionService } from "../../services/exportacionService";
import { useSessionStore } from "../../store/useSessionStore";
import { formatCOP, formatFechaLabel, formatHora, formatUSD } from "../../utils/format";
import { ESTADO_BADGE, CAT_BADGE } from "../../utils/constants";
import { calcularInsumos, formatInsumo } from "../../utils/pedidoHelpers";
import KpiCard from "../../components/shared/KpiCard";
import Pagination from "../../components/shared/Pagination";
import CierreCajaPanel from "../../components/resumen/CierreCajaPanel";

const PAGE_SIZE = 8;

export default function ResumenPage() {
  const qc = useQueryClient();
  const navigate = useNavigate();
  const location = useLocation();
  const usuario = useSessionStore((s) => s.usuario);
  const [hoy, setHoy] = useState(() => new Date().toLocaleDateString("en-CA"));
  const [fecha, setFecha] = useState(() => location.state?.fecha ?? new Date().toLocaleDateString("en-CA"));
  const [paginaPedidos, setPaginaPedidos] = useState(0);
  const [paginaGastos, setPaginaGastos] = useState(0);

  // Limpia el state de navegación entrante (evita que persista al recargar)
  useEffect(() => { if (location.state?.fecha) window.history.replaceState({}, ""); }, []);

  // Actualiza "hoy" exactamente a medianoche
  useEffect(() => {
    const now    = new Date();
    const mañana = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
    const t = setTimeout(() => setHoy(new Date().toLocaleDateString("en-CA")), mañana.getTime() - now.getTime() + 100);
    return () => clearTimeout(t);
  }, [hoy]);

  const { data: r, isLoading, error } = useQuery({
    queryKey: ["resumen", fecha],
    queryFn: () => resumenService.obtener(fecha),
    staleTime: 10_000,
    refetchInterval: 30_000,
  });

  const { data: cierre } = useQuery({
    queryKey: ["cierre-caja", fecha],
    queryFn: () => cierreCajaService.obtener(fecha),
    retry: false,
  });

  const cerrarMut = useMutation({
    mutationFn: cierreCajaService.cerrar,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["cierre-caja", fecha] });
      qc.invalidateQueries({ queryKey: ["resumen", fecha] });
      qc.invalidateQueries({ queryKey: ["fondos"] });
    },
  });

  const pedidosRealizados = useMemo(
    () => (r?.pedidos ?? []).filter((p) => p.estado === "FINALIZADO" || p.estado === "ANULADO").sort((a, b) => b.id - a.id),
    [r]
  );
  const insumosMapResumen = useMemo(() => {
    const map = new Map();
    pedidosRealizados.forEach((p) => map.set(p.id, calcularInsumos(p.detalles ?? [])));
    return map;
  }, [pedidosRealizados]);
  const totalPaginasPedidos = Math.max(1, Math.ceil(pedidosRealizados.length / PAGE_SIZE));
  const paginaActualPedidos = Math.min(paginaPedidos, totalPaginasPedidos - 1);
  const listaPaginaPedidos  = pedidosRealizados.slice(paginaActualPedidos * PAGE_SIZE, (paginaActualPedidos + 1) * PAGE_SIZE);
  const gastos              = r?.gastos ?? [];
  const totalPaginasGastos  = Math.max(1, Math.ceil(gastos.length / PAGE_SIZE));
  const paginaActualGastos  = Math.min(paginaGastos, totalPaginasGastos - 1);
  const listaPaginaGastos   = gastos.slice(paginaActualGastos * PAGE_SIZE, (paginaActualGastos + 1) * PAGE_SIZE);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Resumen del día</h1>
        <div className="flex items-center gap-2">
          <label className="form-label mb-0 text-[13px]">Fecha:</label>
          <input
            type="date" className="form-control w-40"
            value={fecha}
            onChange={(e) => { setFecha(e.target.value); setPaginaPedidos(0); setPaginaGastos(0); }}
          />
          {fecha !== hoy && (
            <button className="btn btn-secondary btn-sm" onClick={() => setFecha(hoy)}>Hoy</button>
          )}
        </div>
        <div className="flex gap-2">
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => exportacionService.descargarResumen(fecha).catch(() => {})}
            title="Descargar resumen del día como CSV"
          >
            <Download size={14} /> Resumen CSV
          </button>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => exportacionService.descargarCaja(fecha).catch(() => {})}
            title="Descargar libro de caja del día como CSV"
          >
            <Download size={14} /> Caja CSV
          </button>
        </div>
      </div>

      {isLoading && <div className="loading-center"><div className="spinner" /></div>}
      {error && <div className="alert alert-error">Error cargando resumen</div>}

      {r && (
        <>
          {/* Pedidos del día */}
          <div className="font-semibold text-verde-oscuro mb-2">Pedidos</div>
          <div className="kpi-grid mb-5">
            <KpiCard label="Total pedidos" value={r.pedidosTotales} icon={ShoppingBag} />
            {r.pedidosFinalizados > 0 && (
              <KpiCard label="Finalizados" value={r.pedidosFinalizados} color="var(--verde-claro)" />
            )}
            {r.pedidosAbiertos > 0 && (
              <KpiCard label="Abiertos" value={r.pedidosAbiertos} color="var(--warning)" />
            )}
            {r.pedidosAnulados > 0 && (
              <KpiCard label="Anulados" value={r.pedidosAnulados} color="var(--danger)" />
            )}
          </div>

          {/* Animales vendidos */}
          {((r.cuyesVendidos ?? 0) > 0 || (r.gallinasVendidas ?? 0) > 0) && (
            <>
              <div className="font-semibold text-verde-oscuro mb-2">Animales vendidos</div>
              <div className="kpi-grid mb-5">
                {(r.cuyesVendidos ?? 0) > 0 && (
                  <KpiCard label="Cuyes vendidos" value={formatInsumo("cuy", r.cuyesVendidos) ?? "0"} />
                )}
                {(r.gallinasVendidas ?? 0) > 0 && (
                  <KpiCard label="Gallinas vendidas" value={formatInsumo("gallina", r.gallinasVendidas) ?? "0"} />
                )}
              </div>
            </>
          )}

          {/* Cobros del día */}
          <div className="font-semibold text-verde-oscuro mb-2">Cobros del día</div>
          <div className="kpi-grid mb-5">
            <KpiCard
              label="Total cobrado"
              value={formatCOP(r.cobradoTotalCop)}
              color="var(--verde-claro)"
              icon={TrendingUp}
            />
            {r.cobradoCop > 0 && (
              <KpiCard label="Efectivo COP" value={formatCOP(r.cobradoCop)} />
            )}
            {r.cobradoUsd > 0 && (
              <KpiCard
                label="Dólares USD"
                value={formatUSD(r.cobradoUsd)}
                sub={`≈ ${formatCOP(r.cobradoTotalCop - r.cobradoCop - r.cobradoNequi)}`}
              />
            )}
            {r.cobradoNequi > 0 && (
              <KpiCard label="NEQUI" value={formatCOP(r.cobradoNequi)} />
            )}
          </div>

          {/* Gastos + Balance */}
          <div className="font-semibold text-verde-oscuro mb-2">Gastos y balance</div>
          <div className="kpi-grid mb-6">
            <KpiCard
              label="Total gastos"
              value={formatCOP(r.gastosTotalCop)}
              color="var(--danger)"
              icon={TrendingDown}
            />
            {r.gastosCop > 0 && (
              <KpiCard label="Gastos COP" value={formatCOP(r.gastosCop)} color="var(--danger)" />
            )}
            {r.gastosUsd > 0 && (
              <KpiCard label="Gastos USD" value={formatUSD(r.gastosUsd)} color="var(--danger)" />
            )}
            <KpiCard
              label="Balance neto"
              value={formatCOP(r.balanceNeto)}
              color={r.balanceNeto >= 0 ? "var(--verde-claro)" : "var(--danger)"}
              icon={DollarSign}
            />
          </div>


          {/* Tabla pedidos del día */}
          <CierreCajaPanel
            resumen={r}
            fecha={fecha}
            cierre={cierre}
            usuario={usuario}
            saving={cerrarMut.isPending}
            onCerrar={(data) => cerrarMut.mutate(data)}
          />

          {cerrarMut.error && (
            <div className="alert alert-error">{cerrarMut.error.message}</div>
          )}

          <div className="card mb-4">
                <div className="card-header">
                  <span className="card-title shrink-0">
                    Pedidos realizados
                    <span className="ml-2 text-[13px] font-normal text-gray-400">{formatFechaLabel(fecha)}</span>
                  </span>
                  <div className="flex-1 flex justify-center">
                    {pedidosRealizados.length > PAGE_SIZE && (
                      <Pagination
                        pagina={paginaActualPedidos}
                        totalItems={pedidosRealizados.length}
                        pageSize={PAGE_SIZE}
                        onChange={setPaginaPedidos}
                      />
                    )}
                  </div>
                  <span className="text-[13px] text-gray-500 shrink-0">
                    {pedidosRealizados.length} pedido{pedidosRealizados.length !== 1 ? "s" : ""}
                  </span>
                </div>
                <div className="table-wrap">
                  {pedidosRealizados.length === 0 ? (
                    <div className="empty-state"><p>Sin pedidos realizados para esta fecha</p></div>
                  ) : (
                    <table>
                      <thead>
                        <tr>
                          <th>#</th>
                          <th>Cliente</th>
                          <th>Estado</th>
                          <th>Productos</th>
                          <th className="text-right">Total</th>
                          <th>Hora</th>
                        </tr>
                      </thead>
                      <tbody>
                        {listaPaginaPedidos.map((p) => {
                          const insumos = insumosMapResumen.get(p.id) ?? {};
                          const gallina = insumos["gallina"];
                          const cuy     = insumos["cuy"];
                          return (
                            <tr key={p.id}
                              className="cursor-pointer hover:bg-gray-50 transition-colors"
                              title="Ver en historial"
                              onClick={() => navigate("/pedidos", { state: { abrirPedidoId: p.id, irAHistorial: true } })}
                            >
                              <td className="font-bold text-gray-500 text-[12px]">#{p.id}</td>
                              <td className="font-semibold">{p.nombre ?? "—"}</td>
                              <td>
                                <span className={`badge ${ESTADO_BADGE[p.estado] ?? "badge-gray"}`}>{p.estado}</span>
                              </td>
                              <td>
                                {(gallina || cuy) ? (
                                  <div className="flex gap-2 flex-wrap text-[12px] font-semibold text-gray-700">
                                    {gallina && <span>🐔 {formatInsumo("gallina", gallina)}</span>}
                                    {cuy     && <span>🐹 {formatInsumo("cuy", cuy)}</span>}
                                  </div>
                                ) : (
                                  <span className="text-[12px] text-gray-500">
                                    {p.detalles?.slice(0, 2).map((d) => d.nombreSnapshot ?? d.nombre).filter(Boolean).join(", ") || "—"}
                                  </span>
                                )}
                              </td>
                              <td className={`text-right font-semibold ${p.estado === "FINALIZADO" ? "text-verde-claro" : "text-gray-400 line-through"}`}>
                                {formatCOP(p.totalCop)}
                              </td>
                              <td className="text-[12px] text-gray-500">
                                {p.finalizadoEn ? formatHora(p.finalizadoEn) : formatHora(p.creadoEn)}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  )}
                </div>
          </div>

          {/* Tabla gastos del día */}
          <div className="card">
                <div className="card-header">
                  <span className="card-title shrink-0">Gastos {formatFechaLabel(fecha)}</span>
                  <div className="flex-1 flex justify-center">
                    {gastos.length > PAGE_SIZE && (
                      <Pagination
                        pagina={paginaActualGastos}
                        totalItems={gastos.length}
                        pageSize={PAGE_SIZE}
                        onChange={setPaginaGastos}
                      />
                    )}
                  </div>
                  <span className="text-[13px] text-gray-500 shrink-0">{gastos.length} registro{gastos.length !== 1 ? "s" : ""}</span>
                </div>
                <div className="table-wrap">
                  {gastos.length === 0 ? (
                    <div className="empty-state"><p>Sin gastos registrados para esta fecha</p></div>
                  ) : (
                    <table>
                      <thead>
                        <tr>
                          <th>Concepto</th>
                          <th>Categoría</th>
                          <th>Moneda</th>
                          <th className="text-right">Monto</th>
                          <th className="text-right">En COP</th>
                          <th>Hora</th>
                        </tr>
                      </thead>
                      <tbody>
                        {listaPaginaGastos.map((g) => (
                          <tr key={g.id}>
                            <td className="font-medium">{g.concepto}</td>
                            <td><span className={`badge ${CAT_BADGE[g.categoria] ?? "badge-gray"}`}>{g.categoria}</span></td>
                            <td><span className="badge badge-warning">{g.moneda}</span></td>
                            <td className="text-right font-semibold">
                              {g.moneda === "USD" ? formatUSD(g.monto) : formatCOP(g.monto)}
                            </td>
                            <td className="text-right">{formatCOP(g.montoCop)}</td>
                            <td className="text-[12px] text-gray-500">{formatHora(g.registradoEn)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
          </div>
        </>
      )}
    </div>
  );
}
