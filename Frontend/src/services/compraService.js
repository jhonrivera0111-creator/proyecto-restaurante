import api from "./api";

export const compraService = {
  listar: (params = {}) => api.get("/compras", { params }).then((r) => r.data),
  obtener: (id) => api.get(`/compras/${id}`).then((r) => r.data),
  registrar: (data) => api.post("/compras", data).then((r) => r.data),
  anular: (id) => api.delete(`/compras/${id}`).then((r) => r.data),
  listarAbonos: (id) => api.get(`/compras/${id}/abonos`).then((r) => r.data),
  registrarAbono: (id, data) =>
    api.post(`/compras/${id}/abonos`, data).then((r) => r.data),
};
