import api from "./api";

const _descargar = async (ruta, filename) => {
  try {
    const response = await api.get(ruta, { responseType: "blob" });
    const blobUrl = URL.createObjectURL(response.data);
    const a = document.createElement("a");
    a.href = blobUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(blobUrl);
  } catch (err) {
    console.error("Error al descargar archivo:", err);
    throw err;
  }
};

export const exportacionService = {
  resumenUrl: (fecha) => `/api/v1/exportaciones/resumen${fecha ? `?fecha=${fecha}` : ""}`,
  cajaUrl: (fecha) => `/api/v1/exportaciones/caja${fecha ? `?fecha=${fecha}` : ""}`,
  descargarResumen: (fecha) =>
    _descargar(
      `/exportaciones/resumen${fecha ? `?fecha=${fecha}` : ""}`,
      `resumen-${fecha ?? "hoy"}.csv`
    ),
  descargarCaja: (fecha) =>
    _descargar(
      `/exportaciones/caja${fecha ? `?fecha=${fecha}` : ""}`,
      `caja-${fecha ?? "hoy"}.csv`
    ),
};
