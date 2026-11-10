import api from "./api";

export const cuentaCobrarService = {
  listar: (params = {}) => api.get("/cuentas-cobrar", { params }).then((r) => r.data),
  obtener: (id) => api.get(`/cuentas-cobrar/${id}`).then((r) => r.data),
  registrar: (data) => api.post("/cuentas-cobrar", data).then((r) => r.data),
  anular: (id) => api.delete(`/cuentas-cobrar/${id}`).then((r) => r.data),
  listarAbonos: (id) => api.get(`/cuentas-cobrar/${id}/abonos`).then((r) => r.data),
  registrarAbono: (id, data) => api.post(`/cuentas-cobrar/${id}/abonos`, data).then((r) => r.data),
};
