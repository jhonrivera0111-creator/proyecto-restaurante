import api from "./api";

export const pedidoService = {
  listar: (todos = false) =>
    api.get("/pedidos", { params: { todos } }).then((r) => r.data),
  obtener: (id) => api.get(`/pedidos/${id}`).then((r) => r.data),
  crear: (data) => api.post("/pedidos", data).then((r) => r.data),
  crearHistorico: (data) => api.post("/pedidos/historico", data).then((r) => r.data),
  actualizar: (id, req) => api.patch(`/pedidos/${id}`, req).then((r) => r.data),
  agregarDetalle: (id, req) =>
    api.post(`/pedidos/${id}/detalles`, req).then((r) => r.data),
  actualizarDetalle: (id, detalleId, req) =>
    api.patch(`/pedidos/${id}/detalles/${detalleId}`, req).then((r) => r.data),
  quitarDetalle: (id, detalleId) =>
    api.delete(`/pedidos/${id}/detalles/${detalleId}`).then((r) => r.data),
  registrarPago: (id, req) =>
    api.post(`/pedidos/${id}/pagos`, req).then((r) => r.data),
  quitarPago: (id, pagoId) =>
    api.delete(`/pedidos/${id}/pagos/${pagoId}`).then((r) => r.data),
  actualizarPago: (id, pagoId, req) =>
    api.patch(`/pedidos/${id}/pagos/${pagoId}`, req).then((r) => r.data),
  finalizar: (id) => api.put(`/pedidos/${id}/finalizar`).then((r) => r.data),
  anular: (id) => api.put(`/pedidos/${id}/anular`).then((r) => r.data),
  fiar: (id) => api.put(`/pedidos/${id}/fiar`).then((r) => r.data),
};
