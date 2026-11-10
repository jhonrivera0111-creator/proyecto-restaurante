import { DollarSign, LogOut, Menu, UserRound } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { useSessionStore } from "../../store/useSessionStore";
import { formatCOP } from "../../utils/format";
import { tasaService } from "../../services/tasaService";

export default function Topbar() {
  const toggleSidebar = useSessionStore((s) => s.toggleSidebar);
  const usuario = useSessionStore((s) => s.usuario);
  const logout = useSessionStore((s) => s.logout);

  const { data: tasa } = useQuery({
    queryKey: ["tasa-vigente"],
    queryFn: tasaService.vigente,
    staleTime: 60_000,
    retry: false,
  });

  const fechaHoy = new Date().toLocaleDateString("es-CO", {
    weekday: "short", day: "2-digit", month: "short", year: "numeric",
  });

  return (
    <header className="topbar">
      <button className="topbar-toggle btn-icon" onClick={toggleSidebar}>
        <Menu size={20} />
      </button>
      <span className="topbar-fecha">{fechaHoy}</span>
      <div className="topbar-spacer" />
      {tasa && (
        <div className="topbar-chip">
          <DollarSign size={14} />
          <span>USD = {formatCOP(tasa.valorCop)}</span>
        </div>
      )}
      {usuario && (
        <div className="topbar-chip bg-gray-100 text-gray-700">
          <UserRound size={14} />
          <span>{usuario.nombre}</span>
        </div>
      )}
      <button className="btn-icon" title="Cambiar usuario" onClick={logout}>
        <LogOut size={17} />
      </button>
    </header>
  );
}
