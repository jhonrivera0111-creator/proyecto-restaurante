import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { tasaService } from "../../../services/tasaService";
import { configuracionService } from "../../../services/configuracionService";
import { formatCOP, formatCreadoEn } from "../../../utils/format";

export default function TasaUsdTab() {
  const qc = useQueryClient();
  const [nuevaTasa, setNuevaTasa] = useState("");
  const [msg, setMsg] = useState(null);

  const { data: vigente } = useQuery({
    queryKey: ["tasa-vigente"],
    queryFn: tasaService.vigente,
  });
  const { data: historial = [] } = useQuery({
    queryKey: ["tasa-historial"],
    queryFn: tasaService.historial,
  });
  const { data: configs = [] } = useQuery({
    queryKey: ["configuracion"],
    queryFn: configuracionService.listar,
  });

  const mostrarTasa = configs.find((c) => c.clave === "mostrar_tasa_dolar")?.valor === "1";

  const mutRegistrar = useMutation({
    mutationFn: () => tasaService.registrar(parseFloat(nuevaTasa)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["tasa-vigente"] });
      qc.invalidateQueries({ queryKey: ["tasa-historial"] });
      setNuevaTasa("");
      setMsg({ tipo: "success", texto: "Tasa actualizada correctamente" });
      setTimeout(() => setMsg(null), 3000);
    },
    onError: (err) => setMsg({ tipo: "error", texto: err.message }),
  });

  const mutToggleTasa = useMutation({
    mutationFn: (valor) => configuracionService.actualizarValor("mostrar_tasa_dolar", valor),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["configuracion"] }),
  });

  const handleGuardar = (e) => {
    e.preventDefault();
    const val = parseFloat(nuevaTasa);
    if (!nuevaTasa || isNaN(val) || val <= 0) {
      setMsg({ tipo: "error", texto: "Ingrese un valor válido mayor a 0" });
      return;
    }
    mutRegistrar.mutate();
  };

  return (
    <div>
      <div className="card mb-5">
        <div className="card-header">
          <span className="card-title">Tasa vigente del dólar</span>
        </div>
        <div className="card-body">
          {vigente ? (
            <div className="mb-4">
              <div className="text-[28px] font-bold text-verde-oscuro">
                {formatCOP(vigente.valorCop)}{" "}
                <span className="text-sm text-gray-500 font-normal">COP / USD</span>
              </div>
              <div className="text-gray-500 text-[13px] mt-1">
                Vigente desde: {formatCreadoEn(vigente.vigenteDesde)}
              </div>
            </div>
          ) : (
            <div className="alert alert-error">No hay tasa registrada</div>
          )}

          {msg && <div className={`alert alert-${msg.tipo}`}>{msg.texto}</div>}

          <form onSubmit={handleGuardar} className="flex gap-2.5 items-end">
            <div className="form-group mb-0 flex-1">
              <label className="form-label">Nueva tasa (COP por 1 USD)</label>
              <input
                className="form-control"
                type="number"
                step="1"
                min="1"
                placeholder="Ej: 4200"
                value={nuevaTasa}
                onChange={(e) => setNuevaTasa(e.target.value)}
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={mutRegistrar.isPending}>
              {mutRegistrar.isPending ? "Guardando…" : "Guardar tasa"}
            </button>
          </form>
        </div>
      </div>

      <div className="card mb-5">
        <div className="card-header"><span className="card-title">Historial de tasas</span></div>
        <div className="table-wrap">
          {historial.length === 0 ? (
            <div className="empty-state"><p>Sin historial</p></div>
          ) : (
            <table>
              <thead>
                <tr><th>Tasa (COP/USD)</th><th>Vigente desde</th></tr>
              </thead>
              <tbody>
                {historial.map((t, i) => (
                  <tr key={t.id}>
                    <td className={i === 0 ? "font-bold" : ""}>{formatCOP(t.valorCop)}</td>
                    <td className="text-gray-500">{formatCreadoEn(t.vigenteDesde)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="card">
        <div className="card-header"><span className="card-title">Visualización</span></div>
        <div className="card-body">
          <div className="form-group mb-0">
            <label className="form-label">Mostrar tasa USD en la barra superior</label>
            <div className="flex items-center gap-3 mt-1">
              <button
                type="button"
                role="switch"
                aria-checked={mostrarTasa}
                className={`relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors focus:outline-none ${
                  mostrarTasa ? "bg-verde-claro" : "bg-gray-300"
                }`}
                onClick={() => mutToggleTasa.mutate(mostrarTasa ? "0" : "1")}
              >
                <span className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                  mostrarTasa ? "translate-x-6" : "translate-x-1"
                }`} />
              </button>
              <span className="text-[13px] text-gray-700 font-medium">
                {mostrarTasa ? "Visible" : "Oculta"}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
