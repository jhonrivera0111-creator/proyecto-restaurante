import { useEffect, useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download, LockKeyhole } from "lucide-react";
import { exportacionService } from "../../services/exportacionService";
import { configuracionService } from "../../services/configuracionService";
import { formatCOP } from "../../utils/format";
import ConfirmDialog from "../shared/ConfirmDialog";

const BILL_ROWS = [
  { key: "billetes100k", label: "$100.000", value: 100_000 },
  { key: "billetes50k",  label: "$50.000",  value: 50_000  },
  { key: "billetes20k",  label: "$20.000",  value: 20_000  },
  { key: "billetes10k",  label: "$10.000",  value: 10_000  },
  { key: "billetes5k",   label: "$5.000",   value: 5_000   },
  { key: "billetes2k",   label: "$2.000",   value: 2_000   },
];

const DENOM_INITIAL = {
  baseEfectivo:    "",
  billetes100k:    "",
  billetes50k:     "",
  billetes20k:     "",
  billetes10k:     "",
  billetes5k:      "",
  billetes2k:      "",
  monedasEfectivo: "",
};

/** Panel de cierre de caja diario. Extraído de ResumenPage. */
export default function CierreCajaPanel({ resumen, fecha, cierre, onCerrar, saving, usuario }) {
  const [denoms, setDenoms]              = useState(DENOM_INITIAL);
  const [contadoUsd, setContadoUsd]     = useState("");
  const [contadoNequi, setContadoNequi] = useState("");
  const [observacion, setObservacion]   = useState("");
  const [confirmando, setConfirmando]   = useState(false);
  const [datosConfirm, setDatosConfirm] = useState(null);

  // Pre-load base_caja_cop from configuration
  const { data: configBase } = useQuery({
    queryKey: ["configuracion", "base_caja_cop"],
    queryFn: () => configuracionService.get("base_caja_cop"),
    retry: false,
    enabled: !cierre,
  });

  // Pre-fill USD and NEQUI from fund balances; reset denoms on date change
  useEffect(() => {
    const saldo = (moneda) => resumen.fondos?.find((f) => f.moneda === moneda)?.saldo ?? 0;
    setContadoUsd(String(saldo("USD")));
    setContadoNequi(String(saldo("NEQUI")));
    setObservacion("");
    setDenoms(DENOM_INITIAL);
  }, [fecha, resumen.fondos]);

  // Pre-fill baseEfectivo once config loads (only if the field is still empty)
  useEffect(() => {
    if (configBase?.valor && denoms.baseEfectivo === "") {
      setDenoms((prev) => ({ ...prev, baseEfectivo: String(configBase.valor) }));
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [configBase]);

  const setDenom = (key) => (e) => setDenoms((prev) => ({ ...prev, [key]: e.target.value }));

  // Real-time total of all denominations
  const totalContadoEfectivo = useMemo(() => {
    const base     = parseFloat(denoms.baseEfectivo) || 0;
    const b100k    = (parseInt(denoms.billetes100k)  || 0) * 100_000;
    const b50k     = (parseInt(denoms.billetes50k)   || 0) * 50_000;
    const b20k     = (parseInt(denoms.billetes20k)   || 0) * 20_000;
    const b10k     = (parseInt(denoms.billetes10k)   || 0) * 10_000;
    const b5k      = (parseInt(denoms.billetes5k)    || 0) * 5_000;
    const b2k      = (parseInt(denoms.billetes2k)    || 0) * 2_000;
    const monedas  = parseFloat(denoms.monedasEfectivo) || 0;
    return base + b100k + b50k + b20k + b10k + b5k + b2k + monedas;
  }, [denoms]);

  // Amount to hand over to owner = total minus the base kept for tomorrow
  const paraEntregar = useMemo(() => {
    return totalContadoEfectivo - (parseFloat(denoms.baseEfectivo) || 0);
  }, [totalContadoEfectivo, denoms.baseEfectivo]);

  const numeroONull = (valor) => (valor === "" || valor == null ? null : Number(valor));
  const bloqueado = resumen.pedidosAbiertos > 0;

  const submit = (e) => {
    e.preventDefault();
    setDatosConfirm({
      fecha,
      usuarioId:    usuario?.id,
      // Denomination breakdown
      baseEfectivo:    numeroONull(denoms.baseEfectivo),
      billetes100k:    numeroONull(denoms.billetes100k),
      billetes50k:     numeroONull(denoms.billetes50k),
      billetes20k:     numeroONull(denoms.billetes20k),
      billetes10k:     numeroONull(denoms.billetes10k),
      billetes5k:      numeroONull(denoms.billetes5k),
      billetes2k:      numeroONull(denoms.billetes2k),
      monedasEfectivo: numeroONull(denoms.monedasEfectivo),
      paraEntregar,
      // Fallback total that backend uses as contadoEfectivo
      contadoEfectivo: totalContadoEfectivo,
      contadoUsd:   numeroONull(contadoUsd),
      contadoNequi: numeroONull(contadoNequi),
      observacion,
    });
    setConfirmando(true);
  };

  // ── Read-only view when cierre already exists ──────────────────────────────
  const hasDesglose = cierre && cierre.billetes100k != null;

  return (
    <div className="card mb-6">
      <div className="card-header">
        <div className="flex items-center gap-2">
          <LockKeyhole size={17} className="text-verde-claro" />
          <span className="card-title">Cierre de caja</span>
        </div>
        <a className="btn btn-secondary btn-sm" href={exportacionService.resumenUrl(fecha)}>
          <Download size={14} />
          CSV resumen
        </a>
      </div>
      <div className="card-body">
        {cierre ? (
          <div>
            <div className="alert alert-success mb-4">
              Caja cerrada por {cierre.usuarioNombre ?? "sistema-local"}.
              {cierre.backupPath && <span> Backup: {cierre.backupPath}</span>}
            </div>

            {hasDesglose ? (
              <table className="w-full text-sm mb-3">
                <thead>
                  <tr className="text-left text-gray-500 border-b">
                    <th className="pb-1 font-medium">Denominación</th>
                    <th className="pb-1 font-medium text-right">Cant.</th>
                    <th className="pb-1 font-medium text-right">Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {cierre.baseEfectivo != null && (
                    <tr className="border-b border-gray-100">
                      <td className="py-1 text-gray-600">Base para mañana</td>
                      <td className="py-1 text-right">—</td>
                      <td className="py-1 text-right font-medium">{formatCOP(cierre.baseEfectivo)}</td>
                    </tr>
                  )}
                  {BILL_ROWS.map(({ key, label, value }) =>
                    cierre[key] > 0 ? (
                      <tr key={key} className="border-b border-gray-100">
                        <td className="py-1 text-gray-600">{label}</td>
                        <td className="py-1 text-right">{cierre[key]}</td>
                        <td className="py-1 text-right">{formatCOP(cierre[key] * value)}</td>
                      </tr>
                    ) : null
                  )}
                  {cierre.monedasEfectivo > 0 && (
                    <tr className="border-b border-gray-100">
                      <td className="py-1 text-gray-600">Monedas</td>
                      <td className="py-1 text-right">—</td>
                      <td className="py-1 text-right">{formatCOP(cierre.monedasEfectivo)}</td>
                    </tr>
                  )}
                  <tr className="font-semibold border-t">
                    <td className="pt-2">Total contado Efectivo</td>
                    <td></td>
                    <td className="pt-2 text-right">{formatCOP(cierre.contadoEfectivo)}</td>
                  </tr>
                  {cierre.paraEntregar != null && (
                    <tr className="text-verde-claro font-semibold">
                      <td className="py-1">Para entregar al dueño</td>
                      <td></td>
                      <td className="py-1 text-right">{formatCOP(cierre.paraEntregar)}</td>
                    </tr>
                  )}
                </tbody>
              </table>
            ) : (
              // Historical record without denomination breakdown
              <p className="text-sm text-gray-600">
                Contado Efectivo:{" "}
                <span className="font-semibold">{formatCOP(cierre.contadoEfectivo)}</span>
              </p>
            )}
          </div>
        ) : (
          <form onSubmit={submit}>
            {bloqueado && (
              <div className="alert alert-error">
                Hay pedidos abiertos. Cierra o anula pendientes antes de cerrar caja.
              </div>
            )}

            {/* ── Denomination grid ─────────────────────────────── */}
            <div className="form-group mb-4">
              <label className="form-label">Contado Efectivo — desglose de billetes</label>
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-gray-500 border-b">
                    <th className="pb-1 font-medium">Denominación</th>
                    <th className="pb-1 font-medium text-right">Cantidad</th>
                    <th className="pb-1 font-medium text-right">Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  {/* Base para mañana */}
                  <tr className="border-b border-gray-100">
                    <td className="py-1 text-gray-700 font-medium">Base para mañana</td>
                    <td className="py-1 text-right">
                      <input
                        className="form-control text-right"
                        style={{ width: "110px", display: "inline-block" }}
                        type="number"
                        step="1000"
                        min="0"
                        placeholder="0"
                        value={denoms.baseEfectivo}
                        onChange={setDenom("baseEfectivo")}
                      />
                    </td>
                    <td className="py-1 text-right font-medium">
                      {formatCOP(parseFloat(denoms.baseEfectivo) || 0)}
                    </td>
                  </tr>

                  {/* Bill rows */}
                  {BILL_ROWS.map(({ key, label, value }) => (
                    <tr key={key} className="border-b border-gray-100">
                      <td className="py-1 text-gray-600">{label}</td>
                      <td className="py-1 text-right">
                        <input
                          className="form-control text-right"
                          style={{ width: "110px", display: "inline-block" }}
                          type="number"
                          step="1"
                          min="0"
                          placeholder="0"
                          value={denoms[key]}
                          onChange={setDenom(key)}
                        />
                      </td>
                      <td className="py-1 text-right">
                        {formatCOP((parseInt(denoms[key]) || 0) * value)}
                      </td>
                    </tr>
                  ))}

                  {/* Monedas */}
                  <tr className="border-b border-gray-100">
                    <td className="py-1 text-gray-600">Monedas (valor total)</td>
                    <td className="py-1 text-right">
                      <input
                        className="form-control text-right"
                        style={{ width: "110px", display: "inline-block" }}
                        type="number"
                        step="100"
                        min="0"
                        placeholder="0"
                        value={denoms.monedasEfectivo}
                        onChange={setDenom("monedasEfectivo")}
                      />
                    </td>
                    <td className="py-1 text-right">
                      {formatCOP(parseFloat(denoms.monedasEfectivo) || 0)}
                    </td>
                  </tr>

                  {/* Totals footer */}
                  <tr className="font-semibold border-t">
                    <td className="pt-2" colSpan={2}>Total contado Efectivo</td>
                    <td className="pt-2 text-right">{formatCOP(totalContadoEfectivo)}</td>
                  </tr>
                  <tr className="text-verde-claro font-semibold">
                    <td className="py-1" colSpan={2}>Para entregar al dueño</td>
                    <td className="py-1 text-right">{formatCOP(paraEntregar)}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            {/* ── USD & NEQUI ───────────────────────────────────── */}
            <div className="form-row">
              <div className="form-group">
                <label className="form-label">Contado USD</label>
                <input
                  className="form-control"
                  type="number"
                  step="0.01"
                  value={contadoUsd}
                  onChange={(e) => setContadoUsd(e.target.value)}
                />
              </div>
              <div className="form-group">
                <label className="form-label">Contado Nequi</label>
                <input
                  className="form-control"
                  type="number"
                  step="1"
                  value={contadoNequi}
                  onChange={(e) => setContadoNequi(e.target.value)}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Observación</label>
              <input
                className="form-control"
                value={observacion}
                onChange={(e) => setObservacion(e.target.value)}
              />
            </div>

            <button className="btn btn-primary" disabled={saving || bloqueado}>
              {saving ? "Cerrando..." : "Cerrar caja y crear backup"}
            </button>
          </form>
        )}
      </div>

      {confirmando && (
        <ConfirmDialog
          emoji="🔒"
          titulo="¿Cerrar la caja del día?"
          descripcion="Esta acción es irreversible. Se registrará el cierre, se bloqueará el día y se creará un backup automático."
          labelOk="Sí, cerrar caja"
          onOk={() => { setConfirmando(false); onCerrar(datosConfirm); }}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}
