import { create } from "zustand";

// Estado global (Zustand). Solo guarda lo mínimo: quién inició sesión y si el
// menú lateral está colapsado. Los datos del servidor NO van aquí (los maneja
// React Query). La sesión vive solo en memoria: al recargar la página se pide login.
export const useSessionStore = create((set) => ({
  usuario: null, // { id, nombre, rol, token } o null si no hay sesión
  setUsuario: (usuario) => set({ usuario }),
  logout: () => set({ usuario: null }),
  sidebarColapsado: false,
  toggleSidebar: () => set((s) => ({ sidebarColapsado: !s.sidebarColapsado })),
}));
