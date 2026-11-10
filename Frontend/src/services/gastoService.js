import api from "./api";

export const gastoService = {
  listar: (fecha) =>
    api.get("/gastos", { params: fecha ? { fecha } : {} }).then((r) => r.data),
  registrar: (data) => api.post("/gastos", data).then((r) => r.data),
  eliminar: (id) => api.delete(`/gastos/${id}`).then((r) => r.data),
};
