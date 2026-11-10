import api from "./api";

export const fondoService = {
  listar: () => api.get("/fondos").then((r) => r.data),
  historial: (params = {}) => api.get("/fondos/historial", { params }).then((r) => r.data),
  ajustar: (moneda, nuevoSaldo, motivo) =>
    api.put(`/fondos/${moneda}`, { nuevoSaldo, motivo }).then((r) => r.data),
  registrarEntrada: (data) => api.post("/fondos/entrada", data).then((r) => r.data),
  registrarTransferencia: (data) => api.post("/fondos/transferencia", data).then((r) => r.data),
  estadoNequiMes: (fondoMoneda) =>
    api.get("/fondos/nequi/estado-mes", { params: fondoMoneda ? { fondo: fondoMoneda } : {} }).then((r) => r.data),
  crearCuentaNequi: (alias) => api.post("/fondos/nequi/cuentas", { alias }).then((r) => r.data),
  eliminarCuentaNequi: (moneda) => api.delete(`/fondos/nequi/cuentas/${moneda}`),
};
