import api from "./api";

export const usuarioService = {
  listar: () => api.get("/usuarios").then((r) => r.data),
  listarTodos: () => api.get("/usuarios", { params: { todos: true } }).then((r) => r.data),
  login: (usuarioId, password) =>
    api.post("/sesion/login", { usuarioId, password }).then((r) => r.data),
  crearOperador: (nombre) => api.post("/usuarios", { nombre }).then((r) => r.data),
  toggle: (id) => api.put(`/usuarios/${id}/toggle`).then((r) => r.data),
  cambiarPassword: (id, nuevaPassword) =>
    api.put(`/usuarios/${id}/password`, { nuevaPassword }).then((r) => r.data),
  eliminar: (id) => api.delete(`/usuarios/${id}`).then((r) => r.data),
};
