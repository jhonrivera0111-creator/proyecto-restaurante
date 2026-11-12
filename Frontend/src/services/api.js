import axios from "axios";
import { useSessionStore } from "../store/useSessionStore";

// ---------------------------------------------------------------------------
// Cliente HTTP único (Axios) que usan TODOS los servicios del frontend.
// baseURL relativa "/api/v1": en desarrollo Vite la redirige al backend (:5020);
// en Docker la redirige nginx. Así no hay que cambiar URLs entre entornos.
// ---------------------------------------------------------------------------
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? "/api/v1",
  headers: { "Content-Type": "application/json" },
  timeout: 10000,
});

// Interceptor de SALIDA: antes de enviar cualquier petición, agrega el token de
// sesión (guardado al hacer login) en la cabecera Authorization.
api.interceptors.request.use((config) => {
  const token = useSessionStore.getState().usuario?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Interceptor de RESPUESTA: si el backend responde 401 (token vencido/inválido)
// cierra la sesión y vuelve al login; si hay error, lanza un Error cuyo texto es
// el "mensaje" que envía el backend (GlobalExceptionHandler) para mostrarlo al usuario.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      useSessionStore.getState().logout();
    }
    const mensaje = err.response?.data?.mensaje || err.message || "Error de conexión";
    return Promise.reject(new Error(mensaje));
  }
);

export default api;
