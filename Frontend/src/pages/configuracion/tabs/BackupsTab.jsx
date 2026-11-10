import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Save } from "lucide-react";
import { backupService } from "../../../services/backupService";
import { formatCreadoEn } from "../../../utils/format";
import ConfirmDialog from "../../../components/shared/ConfirmDialog";

function formatBytes(bytes) {
  if (bytes == null) return "—";
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

export default function BackupsTab() {
  const qc = useQueryClient();
  const [confirmando, setConfirmando] = useState(false);

  const { data: backups = [], isLoading } = useQuery({
    queryKey: ["backups"],
    queryFn: backupService.listar,
    staleTime: 60_000,
  });

  const mutCrear = useMutation({
    mutationFn: () => backupService.crear("manual"),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["backups"] }),
  });

  return (
    <div className="flex flex-col gap-4">
      <div className="toolbar">
        <div className="flex-1" />
        <button
          className="btn btn-primary"
          onClick={() => setConfirmando(true)}
          disabled={mutCrear.isPending}
        >
          <Save size={15} /> Crear backup manual
        </button>
      </div>

      <div className="card">
        <div className="card-body p-0">
          {isLoading ? (
            <div className="loading-center py-10">
              <span className="spinner" />
            </div>
          ) : backups.length === 0 ? (
            <div className="empty-state py-10">No hay backups disponibles.</div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Nombre</th>
                    <th>Tamaño</th>
                    <th>Fecha</th>
                  </tr>
                </thead>
                <tbody>
                  {backups.map((b) => (
                    <tr key={b.nombre}>
                      <td className="font-medium">{b.nombre}</td>
                      <td className="whitespace-nowrap">{formatBytes(b.bytes)}</td>
                      <td className="whitespace-nowrap text-[13px]">
                        {formatCreadoEn(b.creadoEn)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {confirmando && (
        <ConfirmDialog
          emoji="💾"
          titulo="Crear backup manual"
          descripcion="Se creará una copia de seguridad de la base de datos en este momento."
          labelOk="Crear backup"
          onOk={() => { mutCrear.mutate(); setConfirmando(false); }}
          onCancelar={() => setConfirmando(false)}
        />
      )}
    </div>
  );
}
