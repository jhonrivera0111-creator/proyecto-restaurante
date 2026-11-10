import { create } from "zustand";

export const useSessionStore = create((set) => ({
  usuario: null,
  setUsuario: (usuario) => set({ usuario }),
  logout: () => set({ usuario: null }),
  sidebarColapsado: false,
  toggleSidebar: () => set((s) => ({ sidebarColapsado: !s.sidebarColapsado })),
}));
