import { NavLink } from "react-router-dom";
import {
  LayoutDashboard, ClipboardList, CheckSquare,
  Package, Wallet, Settings, Users, HandCoins,
} from "lucide-react";
import { useSessionStore } from "../../store/useSessionStore";

const links = [
  { to: "/",               icon: LayoutDashboard, label: "Dashboard" },
  { to: "/pedidos",        icon: ClipboardList,   label: "Pedidos" },
  { to: "/resumen",        icon: CheckSquare,     label: "Resumen del día" },
  { to: "/inventario",     icon: Package,         label: "Inventario" },

  { to: "/fondos",         icon: Wallet,          label: "Fondos" },
  { to: "/cuentas-cobrar", icon: HandCoins,       label: "Cuentas x Cobrar" },
  { to: "/empleados",      icon: Users,           label: "Empleados" },
  { to: "/configuracion",  icon: Settings,        label: "Configuración" },
];

export default function Sidebar() {
  const colapsado = useSessionStore((s) => s.sidebarColapsado);

  return (
    <aside className={`sidebar${colapsado ? " colapsado" : ""}`}>
      <div className="sidebar-header">
        <div className="sidebar-title">🌿 Casa Verde</div>
        <div className="sidebar-sub">Panel de operación</div>
      </div>
      <nav className="sidebar-nav">
        {links.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === "/"}
            className={({ isActive }) => `nav-item${isActive ? " activo" : ""}`}
          >
            <Icon />
            <span className="nav-label">{label}</span>
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        Hecho por Harold Velasquez
        <div className="sidebar-footer-version">Versión {__APP_VERSION__}</div>
      </div>
    </aside>
  );
}
