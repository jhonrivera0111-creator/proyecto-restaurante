import api from "./api";

export const salonService = {
  listar: () => api.get("/salones").then((r) => r.data),
  crear: (data) => api.post("/salones", data).then((r) => r.data),
  actualizar: (id, data) => api.put(`/salones/${id}`, data).then((r) => r.data),
  toggle: (id) => api.put(`/salones/${id}/toggle`).then((r) => r.data),
  eliminar: (id) => api.delete(`/salones/${id}`).then((r) => r.data),
};
