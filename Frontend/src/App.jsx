import { createBrowserRouter, createHashRouter, RouterProvider } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AppLayout from "./components/layout/AppLayout";
import UpdateBanner from "./components/shared/UpdateBanner";
import Dashboard from "./pages/dashboard/Dashboard";
import ConfiguracionPage from "./pages/configuracion/ConfiguracionPage";
import InventarioPage from "./pages/inventario/InventarioPage";
import PedidosPage from "./pages/pedidos/PedidosPage";

import ResumenPage from "./pages/resumen/ResumenPage";
import FondosPage from "./pages/fondos/FondosPage";
import EmpleadosPage from "./pages/empleados/EmpleadosPage";
import CuentasCobrarPage from "./pages/cuentas-cobrar/CuentasCobrarPage";

// ---------------------------------------------------------------------------
// App.jsx — raíz de la aplicación React.
//  1) QueryClient: caché de TanStack Query. staleTime 30 s = los datos se
//     consideran "frescos" 30 s antes de volver a pedirlos; retry 1 = un reintento.
//  2) routes: tabla de pantallas. AppLayout es el "marco" (sidebar + topbar + login);
//     cada pantalla se pinta dentro de él (children).
//  3) Router: en el navegador usa URLs normales; en Electron (archivo local) usa
//     "#/ruta" porque file:// no soporta rutas limpias.
// ---------------------------------------------------------------------------
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30_000 } },
});

function ErrorPage() {
  return (
    <div className="empty-state" style={{ height: "100vh" }}>
      <p className="text-lg font-semibold text-gray-700">Algo salió mal</p>
      <p className="text-sm text-gray-500 mt-1">Recarga la página o contacta al administrador.</p>
      <button className="btn btn-secondary mt-4" onClick={() => window.location.href = "/"}>
        Volver al inicio
      </button>
    </div>
  );
}

function NotFound() {
  return (
    <div className="empty-state" style={{ height: "100vh" }}>
      <p className="text-lg font-semibold text-gray-700">Página no encontrada</p>
      <button className="btn btn-secondary mt-4" onClick={() => window.location.href = "/"}>
        Volver al inicio
      </button>
    </div>
  );
}

const routes = [
  {
    path: "/",
    element: <AppLayout />,
    errorElement: <ErrorPage />,
    children: [
      { index: true,           element: <Dashboard /> },
      { path: "pedidos",       element: <PedidosPage /> },
      { path: "resumen",       element: <ResumenPage /> },
      { path: "inventario",    element: <InventarioPage /> },

      { path: "fondos",        element: <FondosPage /> },
      { path: "configuracion", element: <ConfiguracionPage /> },
      { path: "empleados",       element: <EmpleadosPage /> },
      { path: "cuentas-cobrar", element: <CuentasCobrarPage /> },
      { path: "*",              element: <NotFound /> },
    ],
  },
];

// En modo Electron la app corre desde file:// — createBrowserRouter no resuelve
// rutas correctamente. createHashRouter usa /#/ruta que sí funciona con file://.
const router = import.meta.env.VITE_ELECTRON
  ? createHashRouter(routes)
  : createBrowserRouter(routes);

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
      <UpdateBanner />
    </QueryClientProvider>
  );
}
