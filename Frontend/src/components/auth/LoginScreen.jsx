import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { KeyRound, LogIn, UserRound } from "lucide-react";
import { usuarioService } from "../../services/usuarioService";
import { useSessionStore } from "../../store/useSessionStore";

export default function LoginScreen() {
  const setUsuario = useSessionStore((s) => s.setUsuario);
  const [usuarioId, setUsuarioId] = useState("");
  const [password, setPassword] = useState("");
  const [loginError, setLoginError] = useState("");
  const [saving, setSaving] = useState(false);

  const { data: usuarios = [], isLoading, error: queryError } = useQuery({
    queryKey: ["usuarios"],
    queryFn: usuarioService.listar,
    staleTime: 30_000,
    retry: 1,
  });

  // Primer usuario como default sin useEffect
  const selectedId = usuarioId || (usuarios.length > 0 ? String(usuarios[0].id) : "");
  const usuario = usuarios.find((u) => String(u.id) === selectedId);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoginError("");
    setSaving(true);
    try {
      const sesion = await usuarioService.login(Number(selectedId), password);
      setUsuario(sesion);
    } catch (err) {
      setLoginError(err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-page flex items-center justify-center p-6">
      <div className="card w-full max-w-[420px]">
        <div className="card-header">
          <div>
            <div className="card-title">Casa Verde</div>
            <div className="text-[12px] text-gray-500 mt-1">Selecciona usuario local</div>
          </div>
          <UserRound size={22} className="text-verde-claro" />
        </div>
        <form onSubmit={handleSubmit} className="card-body">
          {(loginError || queryError) && (
            <div className="alert alert-error">{loginError || queryError.message}</div>
          )}
          {isLoading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : (
            <>
              <div className="form-group">
                <label className="form-label">Usuario</label>
                <select
                  className="form-control"
                  value={selectedId}
                  onChange={(e) => {
                    setUsuarioId(e.target.value);
                    setPassword("");
                  }}
                >
                  {usuarios.map((u) => (
                    <option key={u.id} value={u.id}>{u.nombre} - {u.rol}</option>
                  ))}
                </select>
              </div>

              {usuario?.requierePassword && (
                <div className="form-group">
                  <label className="form-label" htmlFor="password">Clave administrador</label>
                  <div className="relative">
                    <KeyRound size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                    <input
                      id="password"
                      className="form-control pl-9"
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      autoFocus
                    />
                  </div>
                </div>
              )}

              <button className="btn btn-primary w-full justify-center" disabled={saving || !selectedId}>
                <LogIn size={16} />
                {saving ? "Entrando..." : "Entrar"}
              </button>
            </>
          )}
        </form>
      </div>
    </div>
  );
}
