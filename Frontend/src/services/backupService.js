import api from "./api";

export const backupService = {
  listar: () => api.get("/backups").then((r) => r.data),
  crear: (motivo = "manual") =>
    api.post("/backups", null, { params: { motivo } }).then((r) => r.data),
};
