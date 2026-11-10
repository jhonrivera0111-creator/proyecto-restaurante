import { Outlet } from "react-router-dom";
import Sidebar from "./Sidebar";
import Topbar from "./Topbar";
import LoginScreen from "../auth/LoginScreen";
import { useSessionStore } from "../../store/useSessionStore";

export default function AppLayout() {
  const usuario = useSessionStore((s) => s.usuario);

  if (!usuario) {
    return <LoginScreen />;
  }

  return (
    <div className="layout">
      <Sidebar />
      <div className="main">
        <Topbar />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
