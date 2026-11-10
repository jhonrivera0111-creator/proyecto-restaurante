import api from "./api";

export const tasaService = {
  vigente: () => api.get("/tasa-cambio/vigente").then((r) => r.data),
  historial: () => api.get("/tasa-cambio/historial").then((r) => r.data),
  registrar: (valorCop) => api.post("/tasa-cambio", { valorCop }).then((r) => r.data),
};
