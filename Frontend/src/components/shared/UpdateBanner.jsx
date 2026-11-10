import { useEffect, useState } from "react";
import { Download, CheckCircle2 } from "lucide-react";

export default function UpdateBanner() {
  const [status, setStatus] = useState(null);

  useEffect(() => {
    if (!window.casaverde?.onUpdateStatus) return;
    return window.casaverde.onUpdateStatus(setStatus);
  }, []);

  if (!status) return null;

  return (
    <div className="fixed bottom-4 right-4 z-50 flex items-center gap-2.5 rounded-lg bg-verde-oscuro px-4 py-3 text-white text-[13px] shadow-lg animate-slide-up">
      {status.estado === "descargando" ? (
        <>
          <Download size={16} className="text-verde-hover shrink-0" />
          <span>Descargando actualización… {status.porcentaje ?? 0}%</span>
        </>
      ) : (
        <>
          <CheckCircle2 size={16} className="text-verde-hover shrink-0" />
          <span>Actualización lista — cerrá la app para instalarla</span>
        </>
      )}
    </div>
  );
}
