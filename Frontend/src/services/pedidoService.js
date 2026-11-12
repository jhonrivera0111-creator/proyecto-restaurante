import api from "./api";

// Cliente de la API de pedidos: una función por endpoint de PedidoController.
// Cada una llama al backend con Axios y devuelve directamente el JSON (r.data).
// No contiene lógica: las pantallas las usan a través de React Query
// (useQuery para leer, useMutation para escribir; ver hooks/usePedidoMutations.js).
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
