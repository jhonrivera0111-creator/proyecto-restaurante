import axios from "axios";
import { useSessionStore } from "../store/useSessionStore";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? "/api/v1",
  headers: { "Content-Type": "application/json" },
  timeout: 10000,
});

api.interceptors.request.use((config) => {
  const token = useSessionStore.getState().usuario?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

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
