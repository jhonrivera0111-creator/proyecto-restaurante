import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Power, Trash2 } from "lucide-react";
import { usuarioService } from "../../../services/usuarioService";
import ModalCrearOperador from "../../../components/configuracion/ModalCrearOperador";
import ModalCambiarPassword from "../../../components/configuracion/ModalCambiarPassword";
import ConfirmDialog from "../../../components/shared/ConfirmDialog";

export default function UsuariosTab() {
  const qc = useQueryClient();
  const [creando, setCreando]                     = useState(false);
  const [cambiandoPassword, setCambiandoPassword] = useState(null);
  const [eliminandoId, setEliminandoId]           = useState(null);
  const [msg, setMsg]                             = useState(null);

  const { data: usuarios = [], isLoading } = useQuery({
    queryKey: ["usuarios-todos"],
    queryFn:  usuarioService.listarTodos,
  });

  const invalidar = () => qc.invalidateQueries({ queryKey: ["usuarios-todos"] });

  const flash = (tipo, texto) => {
    setMsg({ tipo, texto });
    setTimeout(() => setMsg(null), 2500);
  };

  const mutCrear = useMutation({
    mutationFn: (nombre) => usuarioService.crearOperador(nombre),
    onSuccess: () => { invalidar(); flash("success", "Operador creado"); },
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  const mutToggle = useMutation({
    mutationFn: (id) => usuarioService.toggle(id),
    onSuccess: () => invalidar(),
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  const mutPassword = useMutation({
    mutationFn: ({ id, nuevaPassword }) => usuarioService.cambiarPassword(id, nuevaPassword),
    onSuccess: () => { invalidar(); flash("success", "Contraseña actualizada"); },
    onError:   (err) => flash("error", err.response?.data?.mensaje ?? err.message),
  });

  const mutEliminar = useMutation({
    mutationFn: (id) => usuarioService.eliminar(id),
    onSuccess: () => { invalidar(); setEliminandoId(null); flash("success", "Operador eliminado"); },
    onError:   (err) => { setEliminandoId(null); flash("error", err.response?.data?.mensaje ?? err.message); },
  });

  if (isLoading) return <div className="loading-center"><div className="spinner" /></div>;

  return (
    <div>
      {msg && <div className={`alert alert-${msg.tipo} mb-4`}>{msg.texto}</div>}

      <div className="card">
        <div className="card-header">
          <span className="card-title">Usuarios del sistema</span>
          <button
            className="btn btn-primary btn-sm flex items-center gap-1 ml-auto"
            onClick={() => setCreando(true)}
          >
            <Plus size={14} /> Nuevo operador
          </button>
        </div>
        <div className="table-wrap">
          {usuarios.length === 0 ? (
            <div className="empty-state"><p>Sin usuarios</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Rol</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {usuarios.map((u) => (
                  <tr key={u.id}>
                    <td className="font-medium">{u.nombre}</td>
                    <td>
                      <span className={`badge ${u.rol === "ADMIN" ? "badge-warning" : "badge-info"}`}>
                        {u.rol}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${u.activo ? "badge-success" : "badge-gray"}`}>
                        {u.activo ? "Activo" : "Inactivo"}
                      </span>
                    </td>
                    <td>
                      <div className="flex gap-2 items-center">
                        {u.requierePassword && (
                          <button
                            className="btn btn-secondary btn-sm"
                            onClick={() => setCambiandoPassword(u)}
                          >
                            Cambiar clave
                          </button>
                        )}
                        {u.rol !== "ADMIN" && (
                          <>
                            <button
                              className="btn-icon"
                              title={u.activo ? "Desactivar" : "Activar"}
                              disabled={mutToggle.isPending}
                              onClick={() => mutToggle.mutate(u.id)}
                            >
                              <Power size={15} className={u.activo ? "text-green-600" : "text-red-500"} />
                            </button>
                            <button
                              className="btn-icon text-red-400 hover:text-red-600"
                              title="Eliminar operador"
                              disabled={mutEliminar.isPending}
                              onClick={() => setEliminandoId(u.id)}
                            >
                              <Trash2 size={15} />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {creando && (
        <ModalCrearOperador
          onClose={() => setCreando(false)}
          onGuardar={(nombre) => mutCrear.mutateAsync(nombre)}
        />
      )}

      {cambiandoPassword && (
        <ModalCambiarPassword
          usuario={cambiandoPassword}
          onClose={() => setCambiandoPassword(null)}
          onGuardar={(nuevaPassword) =>
            mutPassword.mutateAsync({ id: cambiandoPassword.id, nuevaPassword })
          }
        />
      )}

      {eliminandoId !== null && (() => {
        const u = usuarios.find((x) => x.id === eliminandoId);
        return u ? (
          <ConfirmDialog
            emoji="👤"
            titulo={`Eliminar operador "${u.nombre}"`}
            descripcion="El operador se eliminará permanentemente y no podrá iniciar sesión."
            labelOk="Eliminar"
            labelCancelar="Cancelar"
            peligro
            loading={mutEliminar.isPending}
            onOk={() => mutEliminar.mutate(eliminandoId)}
            onCancelar={() => setEliminandoId(null)}
          />
        ) : null;
      })()}
    </div>
  );
}
