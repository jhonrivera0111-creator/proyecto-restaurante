import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

vi.mock("../../../../services/inventarioService", () => ({
  inventarioService: {
    listarStock: vi.fn(),
    registrarMovimiento: vi.fn(),
  },
}));

import InventarioTab from "../InventarioTab";
import { inventarioService } from "../../../../services/inventarioService";

const STOCK = [
  { insumo: "gallina", stockActual: 12 },
  { insumo: "cuy",     stockActual: 5  },
];

function wrapper({ children }) {
  return (
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      {children}
    </QueryClientProvider>
  );
}

describe("InventarioTab", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    inventarioService.listarStock.mockResolvedValue(STOCK);
  });

  it("muestra el título 'Ajuste de stock'", async () => {
    render(<InventarioTab />, { wrapper });
    expect(await screen.findByText("Ajuste de stock")).toBeInTheDocument();
  });

  it("muestra el nombre capitalizado de cada insumo", async () => {
    render(<InventarioTab />, { wrapper });
    expect(await screen.findByText("Gallina")).toBeInTheDocument();
    expect(screen.getByText("Cuy")).toBeInTheDocument();
  });

  it("renderiza el input de nuevo stock para cada insumo", async () => {
    render(<InventarioTab />, { wrapper });
    await screen.findByText("Gallina");
    expect(screen.getByLabelText(/nuevo stock de gallina/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/nuevo stock de cuy/i)).toBeInTheDocument();
  });

  it("llama registrarMovimiento con tipo 'ajuste' al guardar gallina", async () => {
    inventarioService.registrarMovimiento.mockResolvedValue({});
    render(<InventarioTab />, { wrapper });

    const input = await screen.findByLabelText(/nuevo stock de gallina/i);
    await userEvent.type(input, "10");
    await userEvent.click(screen.getAllByRole("button", { name: /ajustar/i })[0]);
    await userEvent.click(await screen.findByRole("button", { name: /sí, ajustar/i }));

    await waitFor(() =>
      expect(inventarioService.registrarMovimiento).toHaveBeenCalledWith({
        insumo: "gallina",
        tipo: "ajuste",
        cantidad: 10,
        observacion: "Ajuste manual desde configuración",
      })
    );
  });

  it("muestra 'Stock actualizado' tras guardar con éxito", async () => {
    inventarioService.registrarMovimiento.mockResolvedValue({});
    render(<InventarioTab />, { wrapper });

    const input = await screen.findByLabelText(/nuevo stock de gallina/i);
    await userEvent.type(input, "10");
    await userEvent.click(screen.getAllByRole("button", { name: /ajustar/i })[0]);
    await userEvent.click(await screen.findByRole("button", { name: /sí, ajustar/i }));

    await screen.findByText("Stock actualizado");
  });

  it("limpia el input tras guardar con éxito", async () => {
    inventarioService.registrarMovimiento.mockResolvedValue({});
    render(<InventarioTab />, { wrapper });

    const input = await screen.findByLabelText(/nuevo stock de gallina/i);
    await userEvent.type(input, "10");
    await userEvent.click(screen.getAllByRole("button", { name: /ajustar/i })[0]);
    await userEvent.click(await screen.findByRole("button", { name: /sí, ajustar/i }));

    await waitFor(() => expect(input).toHaveValue(null));
  });

  it("muestra error si se intenta guardar sin valor", async () => {
    render(<InventarioTab />, { wrapper });
    await screen.findByText("Gallina");
    await userEvent.click(screen.getAllByRole("button", { name: /ajustar/i })[0]);
    expect(screen.getByText(/ingrese un valor válido/i)).toBeInTheDocument();
  });

  it("muestra error del backend si el servicio falla", async () => {
    inventarioService.registrarMovimiento.mockRejectedValue(new Error("Error de servidor"));
    render(<InventarioTab />, { wrapper });

    const input = await screen.findByLabelText(/nuevo stock de gallina/i);
    await userEvent.type(input, "10");
    await userEvent.click(screen.getAllByRole("button", { name: /ajustar/i })[0]);
    await userEvent.click(await screen.findByRole("button", { name: /sí, ajustar/i }));

    await screen.findByText("Error de servidor");
  });
});
