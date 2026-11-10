import { useState } from "react";
import { useQuery, useQueries, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Trash2, X } from "lucide-react";
import { fondoService } from "../../../services/fondoService";
import { configuracionService } from "../../../services/configuracionService";
import { formatCOP, formatUSD } from "../../../utils/format";
import ModalAjuste from "../../../components/fondos/ModalAjuste";

export default function FondosTab() {
  const qc = useQueryClient();
  const [nuevaAlias, setNuevaAlias]       = useState("");
  const [creandoCuenta, setCreandoCuenta] = useState(false);
  const [ajustando, setAjustando]         = useState(null);
  const [msg, setMsg]                     = useState(null);

  const { data: fondos = [] } = useQuery({
    queryKey: ["fondos"],
    queryFn:  fondoService.listar,
  });
  const nequiCuentas = fondos.filter((f) => f.moneda.startsWith("NEQUI"));

  const { data: configs = [] } = useQuery({
    queryKey: ["configuracion"],
    queryFn:  configuracionService.listar,
  });
  const cuentaActiva = configs.find((c) => c.clave === "cuenta_nequi_activa")?.valor ?? "";

  const estadosNequi = useQueries({
    queries: nequiCuentas.map((f) => ({
      queryKey: ["nequi-estado-mes", f.moneda],
      queryFn:  () => fondoService.estadoNequiMes(f.moneda),
    })),
  });

  const invalidarFondos = () => qc.invalidateQueries({ queryKey: ["fondos"] });

  const flash = (tipo, texto) => {
    setMsg({ tipo, texto });
    setTimeout(() => setMsg(null), 2500);
  };

  const mutCrearNequi = useMutation({
    mutationFn: (alias) => fondoService.crearCuentaNequi(alias),
    onSuccess: () => {
      invalidarFondos();
      setNuevaAlias("");
      setCreandoCuenta(false);
      flash("success", "Cuenta Nequi creada");
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.response?.data?.message ?? err.message }),
  });

  const mutEliminarNequi = useMutation({
    mutationFn: (moneda) => fondoService.eliminarCuentaNequi(moneda),
    onSuccess: () => { invalidarFondos(); flash("success", "Cuenta eliminada"); },
    onError: (err) => setMsg({ tipo: "error", texto: err.response?.data?.message ?? err.message }),
  });

  const mutCuentaActiva = useMutation({
    mutationFn: (moneda) => configuracionService.actualizarValor("cuenta_nequi_activa", moneda),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["configuracion"] });
      flash("success", "Cuenta activa actualizada");
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.message }),
  });

  const ajustarMut = useMutation({
    mutationFn: ({ moneda, nuevoSaldo, motivo }) =>
      fondoService.ajustar(moneda, nuevoSaldo, motivo),
    onSuccess: () => {
      invalidarFondos();
      flash("success", "Ajuste aplicado");
    },
  });

  return (
    <div>
      {msg && <div className={`alert alert-${msg.tipo}`}>{msg.texto}</div>}

      {/* ── Saldos de fondos ─────────────────────────────────────── */}
      <div className="card">
        <div className="card-header"><span className="card-title">Saldos de fondos</span></div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Fondo</th>
                <th className="text-right">Saldo actual</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {fondos.map((f) => {
                const label = f.moneda === "COP"
                  ? "Banco"
                  : f.moneda === "USD"
                  ? "Dólares"
                  : f.moneda === "EFECTIVO"
                  ? "Efectivo - Caja física"
                  : `Nequi${f.alias ? ` — ${f.alias}` : ""}`;
                return (
                  <tr key={f.moneda}>
                    <td className="font-medium">{label}</td>
                    <td className="text-right font-semibold">
                      {f.moneda === "USD"
                        ? formatUSD(f.saldo ?? 0)
                        : formatCOP(f.saldo ?? 0)}
                    </td>
                    <td className="text-right">
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => setAjustando(f)}
                      >
                        Ajustar
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Cuentas Nequi ───────────────────────────────────────────── */}
      <div className="card mt-4">
        <div className="card-header">
          <span className="card-title">Cuentas Nequi</span>
          <div className="ml-auto">
            {!creandoCuenta ? (
              <button className="btn btn-primary btn-sm flex items-center gap-1"
                onClick={() => setCreandoCuenta(true)}>
                <Plus size={14} /> Nueva cuenta
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <input
                  className="form-control h-8 w-40 text-[13px]"
                  placeholder="Alias (ej. Harold)"
                  autoFocus
                  value={nuevaAlias}
                  onChange={(e) => setNuevaAlias(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && nuevaAlias.trim()) mutCrearNequi.mutate(nuevaAlias.trim());
                    if (e.key === "Escape") { setCreandoCuenta(false); setNuevaAlias(""); }
                  }}
                />
                <button
                  className="btn btn-primary btn-sm"
                  disabled={!nuevaAlias.trim() || mutCrearNequi.isPending}
                  onClick={() => mutCrearNequi.mutate(nuevaAlias.trim())}
                >
                  {mutCrearNequi.isPending ? "Creando…" : "Crear"}
                </button>
                <button className="btn-icon" onClick={() => { setCreandoCuenta(false); setNuevaAlias(""); }}>
                  <X size={16} />
                </button>
              </div>
            )}
          </div>
        </div>
        <div className="table-wrap">
          {nequiCuentas.length === 0 ? (
            <div className="empty-state"><p>Sin cuentas Nequi</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Cuenta</th>
                  <th>Alias</th>
                  <th className="text-right">Saldo</th>
                  <th className="text-right">Usado este mes</th>
                  <th className="text-right">Cupo exento</th>
                  <th>Activa</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {nequiCuentas.map((f, idx) => {
                  const estadoMes   = estadosNequi[idx]?.data;
                  const limiteNum   = estadoMes ? Number(estadoMes.limiteExento) : 0;
                  const usado       = estadoMes ? Number(estadoMes.retiroAcumulado) : null;
                  const libre       = estadoMes ? Number(estadoMes.disponibleLibre)  : null;
                  const agotado     = libre !== null && libre <= 0;
                  const esPrincipal = f.moneda === "NEQUI";
                  return (
                    <tr key={f.moneda}>
                      <td><code className="text-[12px]">{f.moneda}</code></td>
                      <td className="font-medium">{f.alias ?? "—"}</td>
                      <td className="text-right font-semibold">{formatCOP(f.saldo ?? 0)}</td>
                      <td className="text-right text-[12px]">
                        {estadoMes && limiteNum > 0
                          ? <span>{formatCOP(usado)} <span className="text-gray-400">/ {formatCOP(limiteNum)}</span></span>
                          : <span className="text-gray-400">—</span>}
                      </td>
                      <td className="text-right text-[12px]">
                        {estadoMes && limiteNum > 0 ? (
                          <span className={`font-semibold ${agotado ? "text-red-600" : "text-verde-claro"}`}>
                            {formatCOP(Math.max(0, libre))}
                            {agotado && <span className="ml-1 text-[10px] font-normal">(cobra 4×1000)</span>}
                          </span>
                        ) : (
                          <span className="text-gray-400">—</span>
                        )}
                      </td>
                      <td>
                        <button
                          className={`px-2 py-0.5 rounded text-[12px] font-medium transition-colors ${
                            f.moneda === cuentaActiva
                              ? "bg-verde-claro/15 text-verde-claro cursor-default"
                              : "text-gray-400 hover:text-verde-claro hover:bg-verde-claro/10 cursor-pointer"
                          }`}
                          disabled={f.moneda === cuentaActiva || mutCuentaActiva.isPending}
                          title={f.moneda === cuentaActiva ? "Cuenta activa" : "Usar esta cuenta"}
                          onClick={() => mutCuentaActiva.mutate(f.moneda)}
                        >
                          {f.moneda === cuentaActiva ? "✓ Activa" : "Usar esta"}
                        </button>
                      </td>
                      <td>
                        {!esPrincipal && (
                          <button
                            className="btn-icon text-red-400 hover:text-red-600"
                            title="Eliminar (requiere saldo 0)"
                            disabled={mutEliminarNequi.isPending}
                            onClick={() => {
                              if (window.confirm(`¿Eliminar ${f.moneda} (${f.alias ?? ""})?\nRequiere saldo $0.`)) {
                                mutEliminarNequi.mutate(f.moneda);
                              }
                            }}
                          >
                            <Trash2 size={15} />
                          </button>
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

      {ajustando && (
        <ModalAjuste
          fondo={ajustando}
          onClose={() => setAjustando(null)}
          onAjustar={(moneda, nuevoSaldo, motivo) =>
            ajustarMut.mutateAsync({ moneda, nuevoSaldo, motivo })
          }
        />
      )}
    </div>
  );
}
