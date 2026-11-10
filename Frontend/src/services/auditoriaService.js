import api from "./api";

export const auditoriaService = {
  listar: (params = {}) =>
    api.get("/auditoria", { params }).then((r) => r.data),
};
