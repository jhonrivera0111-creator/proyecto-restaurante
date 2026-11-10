import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { inventarioService } from "../../../services/inventarioService";
import { formatDecimal } from "../../../utils/format";

const MESES = ["Enero","Febrero","Marzo","Abril","Mayo","Junio",
                "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"];

export default function ReportesTab() {
  const [año, setAño] = useState(new Date().getFullYear());

  const { data, isLoading } = useQuery({
    queryKey: ["comparativo", año],
    queryFn: () => inventarioService.comparativo(año),
    staleTime: 60_000,
  });

  const meses = data?.meses ?? [];

  const totales = useMemo(() => ({
    gallina: meses.reduce((s, m) => s + parseFloat(m.gallina ?? 0), 0),
    cuy:     meses.reduce((s, m) => s + parseFloat(m.cuy ?? 0), 0),
  }), [meses]);

  return (
    <div>
      {/* Selector de año */}
      <div className="toolbar">
        <div className="flex items-center gap-2">
          <button
            className="btn btn-secondary btn-sm btn-icon"
            onClick={() => setAño((a) => a - 1)}
          >
            <ChevronLeft size={16} />
          </button>
          <span className="font-semibold text-verde-oscuro w-14 text-center">{año}</span>
          <button
            className="btn btn-secondary btn-sm btn-icon"
            onClick={() => setAño((a) => a + 1)}
            disabled={año >= new Date().getFullYear()}
          >
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Consumo mensual de inventario — {año}</span>
        </div>
        <div className="table-wrap">
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Mes</th>
                  <th className="text-right">🐔 Gallinas consumidas</th>
                  <th className="text-right">🐹 Cuyes consumidos</th>
                </tr>
              </thead>
              <tbody>
                {meses.map((fila) => {
                  const gallinaVal = parseFloat(fila.gallina ?? 0);
                  const cuyVal = parseFloat(fila.cuy ?? 0);
                  const sinDatos = gallinaVal === 0 && cuyVal === 0;
                  return (
                    <tr key={fila.mes} className={sinDatos ? "text-gray-400" : ""}>
                      <td>{MESES[fila.mes - 1]}</td>
                      <td className="text-right">
                        {sinDatos ? "—" : formatDecimal(gallinaVal)}
                      </td>
                      <td className="text-right">
                        {sinDatos ? "—" : formatDecimal(cuyVal)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
              <tfoot>
                <tr className="font-semibold border-t-2">
                  <td>Total {año}</td>
                  <td className="text-right">{formatDecimal(totales.gallina)}</td>
                  <td className="text-right">{formatDecimal(totales.cuy)}</td>
                </tr>
              </tfoot>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
