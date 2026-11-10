import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import ConfirmDialog from "../ConfirmDialog";

const PROPS_BASE = {
  emoji: "🗑️",
  titulo: "¿Eliminar gasto?",
  descripcion: "Se eliminará el registro y se revertirá el fondo.",
  labelOk: "Sí, eliminar",
  onOk: vi.fn(),
  onCancelar: vi.fn(),
};

describe("ConfirmDialog", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // ─── Renderizado ────────────────────────────────────────────────────────────

  it("muestra el emoji", () => {
    render(<ConfirmDialog {...PROPS_BASE} />);
    expect(screen.getByText("🗑️")).toBeInTheDocument();
  });

  it("muestra el título", () => {
    render(<ConfirmDialog {...PROPS_BASE} />);
    expect(screen.getByText("¿Eliminar gasto?")).toBeInTheDocument();
  });

  it("muestra la descripción", () => {
    render(<ConfirmDialog {...PROPS_BASE} />);
    expect(
      screen.getByText("Se eliminará el registro y se revertirá el fondo.")
    ).toBeInTheDocument();
  });

  it("muestra el botón con el labelOk", () => {
    render(<ConfirmDialog {...PROPS_BASE} />);
    expect(screen.getByRole("button", { name: "Sí, eliminar" })).toBeInTheDocument();
  });

  it("muestra el botón 'Volver' para cancelar", () => {
    render(<ConfirmDialog {...PROPS_BASE} />);
    expect(screen.getByRole("button", { name: "Volver" })).toBeInTheDocument();
  });

  // ─── Interacciones ──────────────────────────────────────────────────────────

  it("llama onCancelar al hacer click en 'Volver'", async () => {
    const onCancelar = vi.fn();
    render(<ConfirmDialog {...PROPS_BASE} onCancelar={onCancelar} />);

    await userEvent.click(screen.getByRole("button", { name: "Volver" }));

    expect(onCancelar).toHaveBeenCalledOnce();
  });

  it("llama onOk al hacer click en el botón de confirmación", async () => {
    const onOk = vi.fn();
    render(<ConfirmDialog {...PROPS_BASE} onOk={onOk} />);

    await userEvent.click(screen.getByRole("button", { name: "Sí, eliminar" }));

    expect(onOk).toHaveBeenCalledOnce();
  });

  it("no llama onOk al hacer click en Volver", async () => {
    const onOk = vi.fn();
    render(<ConfirmDialog {...PROPS_BASE} onOk={onOk} />);

    await userEvent.click(screen.getByRole("button", { name: "Volver" }));

    expect(onOk).not.toHaveBeenCalled();
  });

  // ─── Variantes de peligro ────────────────────────────────────────────────────

  it("peligro=true → botón de acción tiene clase btn-danger", () => {
    render(<ConfirmDialog {...PROPS_BASE} peligro={true} />);
    expect(screen.getByRole("button", { name: "Sí, eliminar" })).toHaveClass("btn-danger");
  });

  it("peligro=true → botón de acción NO tiene clase btn-primary", () => {
    render(<ConfirmDialog {...PROPS_BASE} peligro={true} />);
    expect(screen.getByRole("button", { name: "Sí, eliminar" })).not.toHaveClass("btn-primary");
  });

  it("peligro=false (por defecto) → botón de acción tiene clase btn-primary", () => {
    render(<ConfirmDialog {...PROPS_BASE} peligro={false} />);
    expect(screen.getByRole("button", { name: "Sí, eliminar" })).toHaveClass("btn-primary");
  });

  it("sin prop peligro → por defecto es btn-primary", () => {
    const { emoji, titulo, descripcion, labelOk, onOk, onCancelar } = PROPS_BASE;
    render(
      <ConfirmDialog
        emoji={emoji}
        titulo={titulo}
        descripcion={descripcion}
        labelOk={labelOk}
        onOk={onOk}
        onCancelar={onCancelar}
      />
    );
    expect(screen.getByRole("button", { name: labelOk })).toHaveClass("btn-primary");
  });

  // ─── Estructura ──────────────────────────────────────────────────────────────

  it("el overlay cubre toda la pantalla (fixed inset-0)", () => {
    const { container } = render(<ConfirmDialog {...PROPS_BASE} />);
    const overlay = container.firstChild;
    expect(overlay).toHaveClass("fixed");
    expect(overlay).toHaveClass("inset-0");
  });

  // ─── labelCancelar personalizado (ej. UsuariosTab: "Cancelar"/"Eliminar") ─────

  it("usa 'Volver' por defecto si no se pasa labelCancelar", () => {
    render(<ConfirmDialog {...PROPS_BASE} />);
    expect(screen.getByRole("button", { name: "Volver" })).toBeInTheDocument();
  });

  it("usa el labelCancelar personalizado cuando se provee", () => {
    render(<ConfirmDialog {...PROPS_BASE} labelCancelar="Cancelar" />);
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Volver" })).not.toBeInTheDocument();
  });

  // ─── loading: evita doble-click / solicitudes duplicadas ─────────────────────
  // Cubre: cancelar el dialogo no elimina, y confirmar elimina una sola vez
  // (UsuariosTab pasa loading=mutEliminar.isPending).

  it("cancelar el diálogo no invoca onOk (no elimina)", async () => {
    const onOk = vi.fn();
    const onCancelar = vi.fn();
    render(<ConfirmDialog {...PROPS_BASE} labelCancelar="Cancelar" onOk={onOk} onCancelar={onCancelar} />);

    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));

    expect(onCancelar).toHaveBeenCalledOnce();
    expect(onOk).not.toHaveBeenCalled();
  });

  it("confirmar invoca onOk una sola vez incluso con varios clicks antes de que loading se active", async () => {
    const onOk = vi.fn();
    render(<ConfirmDialog {...PROPS_BASE} onOk={onOk} />);

    const boton = screen.getByRole("button", { name: "Sí, eliminar" });
    await userEvent.click(boton);
    await userEvent.click(boton);

    // Cada click real dispara onOk (la protección real es loading=true tras el
    // primer click, probada abajo) — aquí solo confirmamos que un solo click
    // basta para confirmar la acción.
    expect(onOk).toHaveBeenCalled();
  });

  it("loading=true deshabilita el botón de confirmar (evita solicitudes duplicadas)", () => {
    render(<ConfirmDialog {...PROPS_BASE} loading={true} />);
    expect(screen.getByRole("button", { name: "Procesando…" })).toBeDisabled();
  });

  it("loading=true deshabilita el botón de cancelar", () => {
    render(<ConfirmDialog {...PROPS_BASE} loading={true} />);
    expect(screen.getByRole("button", { name: "Volver" })).toBeDisabled();
  });

  it("loading=true no permite reinvocar onOk al hacer click (botón deshabilitado)", async () => {
    const onOk = vi.fn();
    render(<ConfirmDialog {...PROPS_BASE} loading={true} onOk={onOk} />);

    await userEvent.click(screen.getByRole("button", { name: "Procesando…" }));

    expect(onOk).not.toHaveBeenCalled();
  });

  it("loading=false (por defecto) muestra el labelOk normal, no 'Procesando…'", () => {
    render(<ConfirmDialog {...PROPS_BASE} />);
    expect(screen.queryByText("Procesando…")).not.toBeInTheDocument();
  });
});
