import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// Mock del servicio ANTES de importar el componente
vi.mock("../../../services/usuarioService", () => ({
  usuarioService: {
    listar: vi.fn(),
    login: vi.fn(),
  },
}));

import LoginScreen from "../LoginScreen";
import { usuarioService } from "../../../services/usuarioService";
import { useSessionStore } from "../../../store/useSessionStore";

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const USUARIOS = [
  { id: 1, nombre: "Operador", rol: "OPERADOR", requierePassword: false },
  { id: 2, nombre: "Admin",    rol: "ADMIN",    requierePassword: true  },
];

// LoginScreen usa useQuery (["usuarios"]) — necesita un QueryClientProvider
// propio en el test. retry:false para que los tests de error no esperen
// el reintento automático de React Query.
function wrapper({ children }) {
  return (
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      {children}
    </QueryClientProvider>
  );
}

// ─── Suite ────────────────────────────────────────────────────────────────────

describe("LoginScreen", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // Reinicia el store de Zustand para que cada test parta limpio
    useSessionStore.setState({ usuario: null });
    usuarioService.listar.mockResolvedValue(USUARIOS);
  });

  // ─── Carga inicial ─────────────────────────────────────────────────────────

  it("muestra el spinner mientras carga", () => {
    // listar nunca resuelve durante esta prueba
    usuarioService.listar.mockReturnValue(new Promise(() => {}));
    render(<LoginScreen />, { wrapper });
    expect(document.querySelector(".spinner")).toBeInTheDocument();
  });

  it("oculta el spinner tras cargar", async () => {
    render(<LoginScreen />, { wrapper });
    await waitFor(() =>
      expect(document.querySelector(".spinner")).not.toBeInTheDocument()
    );
  });

  it("muestra el título 'Casa Verde'", () => {
    render(<LoginScreen />, { wrapper });
    expect(screen.getByText("Casa Verde")).toBeInTheDocument();
  });

  it("muestra el select de usuarios con las opciones cargadas", async () => {
    render(<LoginScreen />, { wrapper });
    const select = await screen.findByRole("combobox");
    expect(select).toBeInTheDocument();

    const options = screen.getAllByRole("option");
    expect(options).toHaveLength(USUARIOS.length);
    expect(options[0]).toHaveTextContent("Operador");
    expect(options[1]).toHaveTextContent("Admin");
  });

  it("pre-selecciona el primer usuario", async () => {
    render(<LoginScreen />, { wrapper });
    const select = await screen.findByRole("combobox");
    expect(select).toHaveValue(String(USUARIOS[0].id));
  });

  // ─── Campo de contraseña ───────────────────────────────────────────────────

  it("NO muestra campo de contraseña para usuario sin requierePassword", async () => {
    render(<LoginScreen />, { wrapper });
    await screen.findByRole("combobox");
    expect(screen.queryByLabelText(/Clave/i)).not.toBeInTheDocument();
  });

  it("muestra campo de contraseña al seleccionar usuario con requierePassword=true", async () => {
    render(<LoginScreen />, { wrapper });
    const select = await screen.findByRole("combobox");

    await userEvent.selectOptions(select, String(USUARIOS[1].id));

    expect(screen.getByLabelText(/Clave/i)).toBeInTheDocument();
  });

  it("oculta el campo de contraseña al volver a seleccionar usuario sin password", async () => {
    render(<LoginScreen />, { wrapper });
    const select = await screen.findByRole("combobox");

    // Seleccionar admin (con password) y luego volver al operador
    await userEvent.selectOptions(select, String(USUARIOS[1].id));
    await userEvent.selectOptions(select, String(USUARIOS[0].id));

    expect(screen.queryByLabelText(/Clave/i)).not.toBeInTheDocument();
  });

  // ─── Botón Entrar ──────────────────────────────────────────────────────────

  it("el botón 'Entrar' está habilitado cuando hay usuarioId", async () => {
    render(<LoginScreen />, { wrapper });
    await screen.findByRole("combobox");
    expect(screen.getByRole("button", { name: /Entrar/i })).toBeEnabled();
  });

  // ─── Flujo de login exitoso ────────────────────────────────────────────────

  it("llama a login con usuarioId y password vacío para usuario sin contraseña", async () => {
    const sesionMock = { id: 1, nombre: "Operador", rol: "OPERADOR" };
    usuarioService.login.mockResolvedValue(sesionMock);

    render(<LoginScreen />, { wrapper });
    await screen.findByRole("combobox");

    await userEvent.click(screen.getByRole("button", { name: /Entrar/i }));

    await waitFor(() =>
      expect(usuarioService.login).toHaveBeenCalledWith(USUARIOS[0].id, "")
    );
  });

  it("guarda el usuario en el store de Zustand tras login exitoso", async () => {
    const sesionMock = { id: 1, nombre: "Operador", rol: "OPERADOR" };
    usuarioService.login.mockResolvedValue(sesionMock);

    render(<LoginScreen />, { wrapper });
    await screen.findByRole("combobox");

    await userEvent.click(screen.getByRole("button", { name: /Entrar/i }));

    await waitFor(() =>
      expect(useSessionStore.getState().usuario).toEqual(sesionMock)
    );
  });

  it("envía la contraseña al hacer login con usuario que la requiere", async () => {
    const sesionMock = { id: 2, nombre: "Admin", rol: "ADMIN" };
    usuarioService.login.mockResolvedValue(sesionMock);

    render(<LoginScreen />, { wrapper });
    const select = await screen.findByRole("combobox");

    await userEvent.selectOptions(select, String(USUARIOS[1].id));
    const passwordInput = screen.getByLabelText(/Clave/i);
    await userEvent.type(passwordInput, "secreto123");

    await userEvent.click(screen.getByRole("button", { name: /Entrar/i }));

    await waitFor(() =>
      expect(usuarioService.login).toHaveBeenCalledWith(USUARIOS[1].id, "secreto123")
    );
  });

  // ─── Manejo de errores ─────────────────────────────────────────────────────

  it("muestra error si listar falla", async () => {
    usuarioService.listar.mockRejectedValue(new Error("Sin conexión al servidor"));

    render(<LoginScreen />, { wrapper });

    // LoginScreen fija retry:1 en su useQuery — el primer reintento espera
    // ~1s de backoff antes de que el error quede disponible.
    await screen.findByText("Sin conexión al servidor", {}, { timeout: 3000 });
  });

  it("muestra el error del backend si login falla", async () => {
    usuarioService.login.mockRejectedValue(new Error("Contraseña incorrecta"));

    render(<LoginScreen />, { wrapper });
    await screen.findByRole("combobox");

    await userEvent.click(screen.getByRole("button", { name: /Entrar/i }));

    await screen.findByText("Contraseña incorrecta");
  });

  it("re-habilita el botón tras error de login", async () => {
    usuarioService.login.mockRejectedValue(new Error("Error"));

    render(<LoginScreen />, { wrapper });
    await screen.findByRole("combobox");

    await userEvent.click(screen.getByRole("button", { name: /Entrar/i }));
    await screen.findByText("Error");

    expect(screen.getByRole("button", { name: /Entrar/i })).toBeEnabled();
  });
});
