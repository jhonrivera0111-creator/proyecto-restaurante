import api from "./api";

export const resumenService = {
  obtener: (fecha) =>
    api.get("/resumen", { params: fecha ? { fecha } : {} }).then((r) => r.data),
};
