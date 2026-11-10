import api from "./api";

export const cierreCajaService = {
  obtener: (fecha) => api.get(`/cierres-caja/${fecha}`).then((r) => r.data),
  cerrar: (data) => api.post("/cierres-caja", data).then((r) => r.data),
};
