import { useState, useEffect, memo } from "react";
import { CheckCircle, ChevronDown, CreditCard, Pencil } from "lucide-react";
import { formatCOP, formatCreadoEn } from "../../utils/format";
import { ESTADO_BADGE } from "../../utils/constants";
import { calcularInsumos, formatInsumo, permiteCobro, formatReservaDisplay } from "../../utils/pedidoHelpers";

function OrdenCard({ pedido, onEditar, onCobrar, expandidoInicial = false, idx = 0 }) {
  const [expandido, setExpandido] = useState(expandidoInicial);
  useEffect(() => { if (expandidoInicial) setExpandido(true); }, [expandidoInicial]);
  const saldo        = Math.max(0, pedido.saldoPendiente ?? 0);
  // permiteCobro() en vez de esActivo(): dentro de Pedidos nunca hay un
  // FIADO en pantalla (queda excluido de listarActivos/Historial), pero
  // este mismo componente se reusa en Cuentas por Cobrar para mostrarlos —
  // ahí sí necesita los botones de Editar/Cobrar habilitados.
  const abierto      = permiteCobro(pedido.estado);
  const insumos      = calcularInsumos(pedido.detalles);
  const gallina      = insumos["gallina"];
  const cuy          = insumos["cuy"];
  const tieneReserva = !!(pedido.fechaReserva || pedido.horaReserva);
  const enCurso      = pedido.enCurso;
  const nItems       = pedido.detalles?.length ?? 0;
  const pagado       = saldo <= 0 && (pedido.detalles?.length ?? 0) > 0;

  return (
    <div className="animate-fade-in-card" style={{ animationDelay: `${Math.min(idx * 25, 120)}ms` }}>
      <div className={`card p-0 transition-all duration-150 flex flex-col ${expandido ? "ring-2 ring-verde-claro bg-verde-bg shadow-lg" : "hover:shadow-lg"}`}>
        <div className="p-[18px] cursor-pointer select-none" onClick={() => setExpandido((v) => !v)}>
          {/* Fila 1: Nombre + estado */}
          <div className="flex justify-between items-start gap-2 mb-1.5">
            <span className="font-bold text-[15px] min-w-0 truncate leading-tight">
              {pedido.nombre ?? `#${pedido.id}`}
              <span className="ml-1.5 text-gray-400 text-[12px] font-normal">#{pedido.id}</span>
            </span>
            {(() => {
              const label = pedido.estado === "ABONADO_PARCIAL" ? "PENDIENTE" : pedido.estado;
              return <span className={`badge shrink-0 ${ESTADO_BADGE[label] ?? ESTADO_BADGE[pedido.estado] ?? "badge-gray"}`}>{label}</span>;
            })()}
          </div>

          {/* Fila 2a: chips */}
          {(enCurso || pedido.paraLlevar || tieneReserva) && (
            <div className="flex items-center gap-1.5 flex-wrap mb-1">
              {enCurso && (
                <span className="font-bold px-2 py-0.5 rounded-full bg-green-100 text-green-700 border border-green-300 text-[11px] leading-tight">
                  EN CURSO
                </span>
              )}
              {pedido.paraLlevar && (
                <span className="font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 border border-blue-300 text-[11px] leading-tight">
                  PARA LLEVAR
                </span>
              )}
              {tieneReserva && (
                <span className="font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-700 border border-amber-300 text-[11px] leading-tight">
                  RESERVA
                </span>
              )}
            </div>
          )}

          {/* Fila 2b: tiempo + salón + personas */}
          <div className="flex items-center gap-1.5 flex-wrap mb-2.5 text-[12px]">
            {abierto ? (
              tieneReserva ? (
                <span className="text-gray-500">{formatReservaDisplay(pedido.fechaReserva, pedido.horaReserva)}</span>
              ) : (
                <span className="text-gray-400">{formatCreadoEn(pedido.creadoEn)}</span>
              )
            ) : (
              <span className="text-gray-400 flex flex-wrap gap-x-2.5 gap-y-0.5">
                <span>
                  <span className="text-gray-300 font-medium">{tieneReserva ? "Reserva" : "Creado"}</span>
                  {" "}{tieneReserva ? formatReservaDisplay(pedido.fechaReserva, pedido.horaReserva) : formatCreadoEn(pedido.creadoEn)}
                </span>
                {pedido.finalizadoEn && (
                  <span><span className="text-gray-300 font-medium">·  Fin.</span> {formatCreadoEn(pedido.finalizadoEn)}</span>
                )}
              </span>
            )}
            {pedido.salonNombre && (
              <span className="text-gray-400">· {pedido.salonNombre}{pedido.mesa ? `/${pedido.mesa}` : ""}</span>
            )}
            {pedido.cantPersonas && (
              <span className="text-gray-400">· {pedido.cantPersonas} pers.</span>
            )}
          </div>

          {abierto && (gallina || cuy) && (
            <div className="flex gap-2.5 mb-2.5 px-2.5 py-[5px] rounded-md bg-green-600/[0.07] border border-green-600/20 text-[13px] font-semibold">
              {gallina && <span>🐔 {formatInsumo("gallina", gallina)}</span>}
              {cuy     && <span>🐹 {formatInsumo("cuy", cuy)}</span>}
            </div>
          )}

          <div className="flex gap-4 items-end">
            <div>
              <div className="text-[11px] text-gray-500">Total</div>
              <div className={`font-bold text-[14px] ${pagado ? "text-verde-claro" : ""}`}>{formatCOP(pedido.totalCop)}</div>
            </div>
            {abierto && (pedido.totalAbonado ?? 0) > 0 && saldo > 0 && (
              <div>
                <div className="text-[11px] text-gray-500">Abonado</div>
                <div className="font-bold text-[14px] text-verde-claro">{formatCOP(pedido.totalAbonado)}</div>
              </div>
            )}
            {abierto && saldo > 0 && (
              <div>
                <div className="text-[11px] text-gray-500">Saldo</div>
                <div className="font-bold text-[14px] text-red-600">{formatCOP(saldo)}</div>
              </div>
            )}
            <button
              className="ml-auto flex items-center gap-1 text-[12px] text-gray-500 hover:text-verde-oscuro transition-colors"
              onClick={(e) => { e.stopPropagation(); setExpandido((v) => !v); }}
            >
              {nItems} ítem{nItems !== 1 ? "s" : ""}
              <ChevronDown size={13} className={`transition-transform duration-200 ${expandido ? "rotate-180" : ""}`} />
            </button>
          </div>
        </div>

        {/* Ítems expandidos */}
        {expandido && (nItems > 0 || pedido.observacion) && (
          <div className="border-t border-gray-100 animate-slide-down">
            <div className="px-4 py-2">
              {pedido.detalles.map((d) => (
                <div key={d.id} className="flex justify-between py-1 text-[13px] border-b border-gray-50 last:border-0">
                  <span className="text-gray-700">
                    <span className="font-bold text-verde-oscuro mr-1">{d.cantidad}×</span>
                    {d.nombreSnapshot}
                  </span>
                  {d.subtotalCop > 0 && <span className="font-semibold text-gray-800 shrink-0 ml-2">{formatCOP(d.subtotalCop)}</span>}
                </div>
              ))}
              {pedido.observacion && (
                <div className={`text-[12px] text-gray-600 ${nItems > 0 ? "mt-2 pt-2 border-t border-gray-100" : ""}`}>
                  📝 {pedido.observacion}
                </div>
              )}
            </div>
          </div>
        )}

        {abierto && (
          <div className="flex border-t border-gray-100 mt-auto">
            <button
              className="flex-1 flex items-center justify-center gap-1.5 py-2.5 text-[13px] font-medium text-gray-500 hover:bg-gray-50 transition-colors"
              onClick={(e) => { e.stopPropagation(); onEditar(pedido); }}
            >
              <Pencil size={13} /> Editar
            </button>
            {pedido.totalCop > 0 && (
              <>
                <div className="w-px bg-gray-100" />
                {pagado ? (
                  <button
                    className="flex-1 flex items-center justify-center gap-1.5 py-2.5 text-[13px] font-semibold text-verde-claro hover:bg-verde-claro hover:text-white transition-colors rounded-br-xl"
                    onClick={(e) => { e.stopPropagation(); onCobrar(pedido); }}
                  >
                    <CheckCircle size={13} /> Finalizar
                  </button>
                ) : (
                  <button
                    className="flex-1 flex items-center justify-center gap-1.5 py-2.5 text-[13px] font-semibold text-verde-claro hover:bg-verde-claro hover:text-white transition-colors rounded-br-xl"
                    onClick={(e) => { e.stopPropagation(); onCobrar(pedido); }}
                  >
                    <CreditCard size={13} /> Cobrar
                  </button>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

export default memo(OrdenCard, (prev, next) =>
  prev.pedido === next.pedido &&
  prev.onEditar === next.onEditar &&
  prev.onCobrar === next.onCobrar &&
  prev.expandidoInicial === next.expandidoInicial &&
  prev.idx === next.idx
);
