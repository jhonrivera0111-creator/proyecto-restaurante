import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, SlidersHorizontal } from "lucide-react";
import { inventarioService } from "../../services/inventarioService";
import { formatFecha } from "../../utils/format";
import { formatInsumo, capFirst } from "../../utils/pedidoHelpers";
import Pagination from "../../components/shared/Pagination";
import ModalRegistrarCompra from "../../components/inventario/ModalRegistrarCompra";
import ComprasTab from "./tabs/ComprasTab";
import ProveedoresTab from "./tabs/ProveedoresTab";

const PAGE_SIZE = 8;

const TIPO_CONFIG = {
  entrada: { label: "Entrada",  badge: "badge-success", icono: <ArrowDown size={13} /> },
  salida:  { label: "Salida",   badge: "badge-danger",  icono: <ArrowUp size={13} /> },
  ajuste:  { label: "Ajuste",   badge: "badge-warning", icono: <SlidersHorizontal size={13} /> },
};

const TABS = [
  { id: "stock",       label: "Stock" },
  { id: "compras",     label: "Compras" },
  { id: "proveedores", label: "Proveedores" },
];

function StockTab() {
  const navigate = useNavigate();
  const [filtroInsumo, setFiltroInsumo] = useState("");
  const [pagina, setPagina] = useState(0);

  const { data: stock = [], isLoading: loadingStock } = useQuery({
    queryKey: ["inventario-stock"],
    queryFn: inventarioService.listarStock,
  });

  const { data: movimientos = [], isLoading: loadingMov } = useQuery({
    queryKey: ["inventario-movimientos", filtroInsumo],
    queryFn: () => inventarioService.listarMovimientos(filtroInsumo || undefined),
  });

  const totalPaginas   = Math.max(1, Math.ceil(movimientos.length / PAGE_SIZE));
  const paginaActual   = Math.min(pagina, totalPaginas - 1);
  const movimientosPag = movimientos.slice(paginaActual * PAGE_SIZE, (paginaActual + 1) * PAGE_SIZE);

  return (
    <div>
      {/* Stock actual */}
      <div className="kpi-grid mb-6">
        {loadingStock ? (
          <div className="loading-center"><div className="spinner" /></div>
        ) : stock.map((s) => {
          const borderCls = s.stockActual < 2 ? "border-l-red-600" : s.stockActual < 5 ? "border-l-amber-500" : "border-l-verde-claro";
          return (
            <div key={s.insumo} className={`kpi-card ${borderCls}`}>
              <div className="kpi-label">{capFirst(s.insumo)}</div>
              <div className="font-bold text-[18px] leading-snug mt-1">{formatInsumo(s.insumo, s.stockActual) ?? "0"}</div>
            </div>
          );
        })}
      </div>

      {/* Movimientos */}
      <div className="card">
        <div className="card-header">
          <span className="card-title shrink-0">Historial de movimientos</span>
          <div className="flex-1 flex justify-center">
            {movimientos.length > PAGE_SIZE && (
              <Pagination pagina={paginaActual} totalItems={movimientos.length} pageSize={PAGE_SIZE} onChange={setPagina} />
            )}
          </div>
          <div className="flex gap-2.5 items-center shrink-0">
            <select className="form-control w-40" value={filtroInsumo} onChange={(e) => { setFiltroInsumo(e.target.value); setPagina(0); }}>
              <option value="">Todos los insumos</option>
              {stock.map((s) => <option key={s.insumo} value={s.insumo}>{capFirst(s.insumo)}</option>)}
            </select>
          </div>
        </div>
        <div className="table-wrap">
          {loadingMov ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : movimientos.length === 0 ? (
            <div className="empty-state"><p>Sin movimientos registrados</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha / Hora</th>
                  <th>Insumo</th>
                  <th>Tipo</th>
                  <th>Cantidad</th>
                  <th>Stock resultante</th>
                  <th>Observación</th>
                </tr>
              </thead>
              <tbody>
                {movimientosPag.map((m) => {
                  const conf = TIPO_CONFIG[m.tipo] ?? { label: m.tipo, badge: "badge-gray", icono: null };
                  const esSalidaPedido = m.tipo === "salida" && m.referenciaTipo === "pedido" && m.referenciaId;
                  const esGasto = m.referenciaTipo === "gasto" && m.referenciaId;
                  return (
                    <tr key={m.id}
                      className={(esSalidaPedido || esGasto) ? "cursor-pointer hover:bg-gray-50 transition-colors" : ""}
                      title={esSalidaPedido ? "Ver pedido en historial" : esGasto ? "Ver en módulo de gastos" : undefined}
                      onClick={
                        esSalidaPedido ? () => navigate("/pedidos", { state: { abrirPedidoId: Number(m.referenciaId), irAHistorial: true } })
                        : esGasto ? () => navigate("/gastos")
                        : undefined
                      }
                    >
                      <td className="text-gray-500 text-[12px] whitespace-nowrap">{formatFecha(m.registradoEn)}</td>
                      <td className="font-medium">{capFirst(m.insumo)}</td>
                      <td>
                        <span className={`badge ${conf.badge} gap-1`}>
                          {conf.icono} {conf.label}
                        </span>
                      </td>
                      <td className="font-semibold">
                        {m.tipo === "ajuste" ? "→ " : m.tipo === "entrada" ? "+ " : "− "}
                        {formatInsumo(m.insumo, Math.abs(m.cantidad)) ?? "0"}
                      </td>
                      <td>{formatInsumo(m.insumo, m.stockResultante) ?? "0"}</td>
                      <td className="text-gray-500">{m.observacion ?? "—"}</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export default function InventarioPage() {
  const [tab, setTab] = useState("stock");
  const [modalCompraAbierto, setModalCompraAbierto] = useState(false);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Inventario</h1>
        {tab === "stock" && (
          <button className="btn btn-primary" onClick={() => setModalCompraAbierto(true)}>
            + Registrar compra
          </button>
        )}
      </div>

      <div className="tabs">
        {TABS.map((t) => (
          <button
            key={t.id}
            className={`tab${tab === t.id ? " activo" : ""}`}
            onClick={() => setTab(t.id)}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "stock" && <StockTab />}
      {tab === "compras" && <ComprasTab />}
      {tab === "proveedores" && <ProveedoresTab />}

      {modalCompraAbierto && (
        <ModalRegistrarCompra onClose={() => setModalCompraAbierto(false)} />
      )}
    </div>
  );
}
