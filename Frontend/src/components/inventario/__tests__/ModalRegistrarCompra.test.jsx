import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

vi.mock("../../../services/compraService", () => ({
  compraService: { registrar: vi.fn(), registrarAbono: vi.fn() },
}));

vi.mock("../../../services/proveedorService", () => ({
  proveedorService: {
    listar: vi.fn().mockResolvedValue([
      { id: 1, nombre: "Granja El Paisa", tipo: "GALLINA", activo: true },
      { id: 2, nombre: "Cuyera Central", tipo: "CUY", activo: true },
    ]),
    crear: vi.fn(),
  },
}));

import ModalRegistrarCompra from "../ModalRegistrarCompra";
import { compraService } from "../../../services/compraService";
import { proveedorService } from "../../../services/proveedorService";

function wrapper({ children }) {
  return (
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      {children}
    </QueryClientProvider>
  );
}

async function llenarFormularioBasico() {
  await userEvent.selectOptions(screen.getByDisplayValue("Seleccionar…"), "1");
  await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
  await userEvent.type(screen.getByLabelText(/precio unitario/i), "30000");
}

describe("ModalRegistrarCompra", () => {
  let onClose;

  beforeEach(() => {
    onClose = vi.fn();
    vi.clearAllMocks();
  });

  it("muestra el título 'Registrar compra'", () => {
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    expect(screen.getByText("Registrar compra", { selector: ".modal-title" })).toBeInTheDocument();
  });

  it("lista los proveedores activos en el select", async () => {
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    expect(await screen.findByText("Granja El Paisa")).toBeInTheDocument();
    expect(screen.getByText("Cuyera Central")).toBeInTheDocument();
  });

  it("exige seleccionar proveedor antes de registrar", async () => {
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await screen.findByText("Granja El Paisa");
    await userEvent.type(screen.getByLabelText(/cantidad/i), "5");
    await userEvent.type(screen.getByLabelText(/precio unitario/i), "30000");
    await userEvent.click(screen.getByRole("button", { name: /^registrar compra$/i }));
    expect(screen.getByText(/selecciona un proveedor/i)).toBeInTheDocument();
    expect(compraService.registrar).not.toHaveBeenCalled();
  });

  it("registra la compra con el payload correcto", async () => {
    compraService.registrar.mockResolvedValue({ id: 99 });
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await screen.findByText("Granja El Paisa");
    await llenarFormularioBasico();
    await userEvent.click(screen.getByRole("button", { name: /^registrar compra$/i }));

    // mutationFn de TanStack Query recibe un segundo argumento de contexto
    // (client, meta, mutationKey) además de las variables — de ahí el
    // expect.anything() adicional.
    await waitFor(() =>
      expect(compraService.registrar).toHaveBeenCalledWith(
        expect.objectContaining({
          proveedorId: 1,
          insumo: "gallina",
          cantidad: 5,
          precioUnitario: 30000,
        }),
        expect.anything()
      )
    );
    expect(compraService.registrarAbono).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("registra tambien el abono inicial cuando se activa esa opción", async () => {
    compraService.registrar.mockResolvedValue({ id: 99 });
    compraService.registrarAbono.mockResolvedValue({});
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await screen.findByText("Granja El Paisa");
    await llenarFormularioBasico();

    await userEvent.click(screen.getByRole("button", { name: /registrar abono inicial/i }));
    await userEvent.type(screen.getByPlaceholderText(/monto/i), "50000");
    await userEvent.click(screen.getByRole("button", { name: /^registrar compra$/i }));

    await waitFor(() =>
      expect(compraService.registrarAbono).toHaveBeenCalledWith(
        99,
        expect.objectContaining({ monto: 50000, moneda: "COP" })
      )
    );
  });

  it("si el abono falla tras crear la compra, el reintento no duplica la compra", async () => {
    compraService.registrar.mockResolvedValue({ id: 99 });
    compraService.registrarAbono
      .mockRejectedValueOnce(new Error("El fondo COP no tiene saldo suficiente"))
      .mockResolvedValueOnce({});
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await screen.findByText("Granja El Paisa");
    await llenarFormularioBasico();

    await userEvent.click(screen.getByRole("button", { name: /registrar abono inicial/i }));
    await userEvent.type(screen.getByPlaceholderText(/monto/i), "50000");
    await userEvent.click(screen.getByRole("button", { name: /^registrar compra$/i }));

    await screen.findByText(/compra registrada\. el abono no se pudo aplicar/i);
    expect(compraService.registrar).toHaveBeenCalledTimes(1);
    expect(onClose).not.toHaveBeenCalled();

    // Reintentar: el segundo submit debe llamar solo al abono, con la MISMA compra (id 99)
    await userEvent.click(screen.getByRole("button", { name: /reintentar abono/i }));

    await waitFor(() => expect(compraService.registrarAbono).toHaveBeenCalledTimes(2));
    expect(compraService.registrar).toHaveBeenCalledTimes(1);
    expect(compraService.registrarAbono).toHaveBeenLastCalledWith(
      99,
      expect.objectContaining({ monto: 50000 })
    );
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("rechaza un abono mayor al total de la compra", async () => {
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await screen.findByText("Granja El Paisa");
    await llenarFormularioBasico(); // total = 5 * 30000 = 150000

    await userEvent.click(screen.getByRole("button", { name: /registrar abono inicial/i }));
    await userEvent.type(screen.getByPlaceholderText(/monto/i), "999999");
    await userEvent.click(screen.getByRole("button", { name: /^registrar compra$/i }));

    expect(screen.getByText(/el abono no puede ser mayor al total/i)).toBeInTheDocument();
    expect(compraService.registrar).not.toHaveBeenCalled();
  });

  it("abre el modal de nuevo proveedor y selecciona el creado", async () => {
    proveedorService.crear.mockResolvedValue({ id: 3, nombre: "Nueva Granja" });
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await screen.findByText("Granja El Paisa");

    await userEvent.click(screen.getByTitle(/nuevo proveedor/i));
    expect(screen.getByText("Nuevo proveedor")).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText(/nombre/i), "Nueva Granja");
    await userEvent.click(screen.getByRole("button", { name: /^guardar$/i }));

    await waitFor(() => expect(proveedorService.crear).toHaveBeenCalled());
  });

  it("muestra error del backend si el servicio falla", async () => {
    compraService.registrar.mockRejectedValue(new Error("Insumo inválido"));
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await screen.findByText("Granja El Paisa");
    await llenarFormularioBasico();
    await userEvent.click(screen.getByRole("button", { name: /^registrar compra$/i }));

    await screen.findByText("Insumo inválido");
    expect(onClose).not.toHaveBeenCalled();
  });

  it("llama onClose al hacer clic en Cancelar", async () => {
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    await userEvent.click(screen.getByRole("button", { name: /cancelar/i }));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("llama onClose al presionar Escape", () => {
    render(<ModalRegistrarCompra onClose={onClose} />, { wrapper });
    fireEvent.keyDown(document, { key: "Escape" });
    expect(onClose).toHaveBeenCalledOnce();
  });
});
