import api from "./api";

export const empleadoService = {
  listar: (soloActivos = true) =>
    api.get("/empleados", { params: { soloActivos } }).then((r) => r.data),
  crear: (data) => api.post("/empleados", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/empleados/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/empleados/${id}/toggle`).then((r) => r.data),

  listarAdelantos: (id) =>
    api.get(`/empleados/${id}/adelantos`).then((r) => r.data),
  registrarAdelanto: (id, data) =>
    api.post(`/empleados/${id}/adelantos`, data).then((r) => r.data),

  listarDias: (id, soloNoPagados = false) =>
    api.get(`/empleados/${id}/dias`, { params: { soloNoPagados } }).then((r) => r.data),
  registrarDia: (id, data) =>
    api.post(`/empleados/${id}/dias`, data).then((r) => r.data),
  eliminarDia: (id, diaId) =>
    api.delete(`/empleados/${id}/dias/${diaId}`).then((r) => r.data),

  listarPagos: (id) =>
    api.get(`/empleados/${id}/pagos`).then((r) => r.data),
  previewPago: (id) =>
    api.get(`/empleados/${id}/pagos/preview`).then((r) => r.data),
  liquidar: (id, data) =>
    api.post(`/empleados/${id}/pagos`, data).then((r) => r.data),

  listarVacaciones: (id) =>
    api.get(`/empleados/${id}/vacaciones`).then((r) => r.data),
  registrarVacacion: (id, data) =>
    api.post(`/empleados/${id}/vacaciones`, data).then((r) => r.data),
  eliminarVacacion: (id, vacId) =>
    api.delete(`/empleados/${id}/vacaciones/${vacId}`).then((r) => r.data),
};
