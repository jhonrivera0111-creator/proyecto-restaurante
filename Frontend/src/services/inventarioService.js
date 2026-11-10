import api from "./api";

export const inventarioService = {
  listarStock: () => api.get("/inventario").then((r) => r.data),
  listarMovimientos: (insumo) =>
    api.get("/inventario/movimientos", { params: insumo ? { insumo } : {} }).then((r) => r.data),
  registrarMovimiento: (data) => api.post("/inventario/movimiento", data).then((r) => r.data),
  actualizarAlerta: (insumo, alertaMinima) =>
    api.patch(`/inventario/${insumo}/alerta`, { alertaMinima }).then((r) => r.data),
  comparativo: (año) =>
    api.get("/inventario/comparativo", { params: { año } }).then((r) => r.data),
};
