import { useState } from "react";
import { X } from "lucide-react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";
import { cuentaCobrarService } from "../../services/cuentaCobrarService";
import { formatCOP, formatCreadoEn } from "../../utils/format";
import ModalCobrar from "./ModalCobrar";

const ESTADO_BADGE = {
  PENDIENTE:       "badge-gray",
  ABONADO_PARCIAL: "badge-warning",
  CANCELADO:       "badge-success",
};

export default function ModalDetalleCuentaCobrar({ cuentaId, onClose }) {
  const qc = useQueryClient();
  const [cobrando, setCobrando] = useState(false);
  const [error, setError] = useState("");

  useModalKeyboard({ onEscape: onClose });

  const { data: cuenta, isLoading } = useQuery({
    queryKey: ["cuenta-cobrar", cuentaId],
    queryFn: () => cuentaCobrarService.obtener(cuentaId),
  });

  const mutAbono = useMutation({
    mutationFn: (data) => cuentaCobrarService.registrarAbono(cuentaId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["cuenta-cobrar", cuentaId] });
      qc.invalidateQueries({ queryKey: ["cuentas-cobrar"] });
      setError("");
    },
    onError: (err) => setError(err.message),
  });

  return (
    <div className="modal-overlay">
      <div className="modal modal-lg">
        <div className="modal-header">
          <span className="modal-title">Cuenta — {cuenta?.clienteNombre ?? "…"}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>

        <div className="modal-body">
          {error && <div className="alert alert-error mb-3">{error}</div>}

          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : cuenta ? (
            <>
              <div className="grid grid-cols-2 gap-3 mb-4 text-sm">
                <div><span className="text-gray-500">Cliente:</span> <strong>{cuenta.clienteNombre}</strong></div>
                <div>
                  <span className="text-gray-500">Estado:</span>{" "}
                  <span className={`badge ${ESTADO_BADGE[cuenta.estado] ?? "badge-gray"}`}>{cuenta.estado}</span>
                </div>
                <div><span className="text-gray-500">Concepto:</span> {cuenta.concepto ?? "—"}</div>
                <div><span className="text-gray-500">Fecha:</span> {cuenta.fecha}</div>
                {cuenta.celular && <div><span className="text-gray-500">Celular:</span> {cuenta.celular}</div>}
                {cuenta.observacion && (
                  <div className="col-span-2"><span className="text-gray-500">Obs:</span> {cuenta.observacion}</div>
                )}
              </div>

              <div className="flex gap-6 mb-4 text-sm bg-gray-50 rounded p-3">
                <div><span className="text-gray-500">Deuda original:</span> <strong>{formatCOP(cuenta.monto)}</strong></div>
                <div><span className="text-gray-500">Total cobrado:</span> <strong>{formatCOP(cuenta.totalAbonado)}</strong></div>
                <div><span className="text-gray-500">Pendiente:</span> <strong className="text-amber-700">{formatCOP(cuenta.saldoPendiente)}</strong></div>
              </div>

              {cuenta.abonos?.length > 0 && (
                <div className="table-wrap mb-4">
                  <table>
                    <thead>
                      <tr><th>Fecha</th><th>Monto</th><th>Moneda</th><th>Total COP</th><th>Fondo</th><th>Registrado</th></tr>
                    </thead>
                    <tbody>
                      {cuenta.abonos.map((a) => (
                        <tr key={a.id}>
                          <td>{a.fecha}</td>
                          <td>{Number(a.monto).toLocaleString("es-CO")}</td>
                          <td>{a.moneda}</td>
                          <td>{formatCOP(a.montoCop)}</td>
                          <td>{a.fondoAcreditado}</td>
                          <td className="text-xs text-gray-400">{a.creadoEn ? formatCreadoEn(a.creadoEn) : "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {cuenta.estado !== "CANCELADO" && (
                <div className="flex justify-end border-t pt-4">
                  <button className="btn btn-primary" onClick={() => setCobrando(true)}>
                    Registrar cobro
                  </button>
                </div>
              )}
            </>
          ) : null}
        </div>

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>Cerrar</button>
        </div>
      </div>

      {cobrando && cuenta && (
        <ModalCobrar
          cuenta={cuenta}
          onClose={() => setCobrando(false)}
          onSave={mutAbono.mutateAsync}
        />
      )}
    </div>
  );
}
