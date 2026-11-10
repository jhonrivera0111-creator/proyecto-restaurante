import { describe, it, expect } from "vitest";
import { formatCOP, formatDecimal, formatFecha, formatFechaLabel, formatHora } from "../format";

// ─── formatCOP ───────────────────────────────────────────────────────────────

describe("formatCOP", () => {
  it("formatea cero", () => {
    expect(formatCOP(0)).toMatch(/0/);
  });

  it("trata null y undefined como 0", () => {
    expect(formatCOP(null)).toMatch(/0/);
    expect(formatCOP(undefined)).toMatch(/0/);
  });

  it("incluye separador de miles para números grandes", () => {
    // es-CO usa punto como separador de miles → "4.200"
    expect(formatCOP(4200)).toMatch(/4[.,]?200/);
  });

  it("incluye el símbolo de la moneda", () => {
    // Dependiendo del ICU de Node puede ser "$" o "COP"
    expect(formatCOP(1000)).toMatch(/\$|COP/);
  });

  it("no incluye decimales", () => {
    // minimumFractionDigits: 0 — no debe aparecer coma decimal en valores enteros
    const resultado = formatCOP(5000);
    expect(resultado).not.toMatch(/[,.]\d{2}$/);
  });
});

// ─── formatDecimal ────────────────────────────────────────────────────────────

describe("formatDecimal", () => {
  it("formatea enteros sin decimales", () => {
    expect(formatDecimal(1)).toBe("1");
    expect(formatDecimal(10)).toBe("10");
  });

  it("formatea fracciones eliminando ceros al final", () => {
    expect(formatDecimal(0.5)).toBe("0.5");
    expect(formatDecimal(0.25)).toBe("0.25");
    expect(formatDecimal(0.125)).toBe("0.125");
    expect(formatDecimal(1.5)).toBe("1.5");
  });

  it("elimina decimales si el resultado es entero", () => {
    expect(formatDecimal(1.000)).toBe("1");
    expect(formatDecimal(3.0)).toBe("3");
  });

  it("retorna '0' para no-números", () => {
    expect(formatDecimal("texto")).toBe("0");
    expect(formatDecimal(null)).toBe("0");
    expect(formatDecimal(undefined)).toBe("0");
  });
});

// ─── formatFecha ─────────────────────────────────────────────────────────────

describe("formatFecha", () => {
  it("retorna '—' para valores falsy", () => {
    expect(formatFecha(null)).toBe("—");
    expect(formatFecha("")).toBe("—");
    expect(formatFecha(undefined)).toBe("—");
  });

  it("retorna una cadena con separador ' · '", () => {
    const resultado = formatFecha("2024-06-15T10:30:00Z");
    expect(resultado).toContain(" · ");
  });

  it("la parte de hora contiene AM o PM", () => {
    const resultado = formatFecha("2024-06-15T10:30:00Z");
    expect(resultado).toMatch(/AM|PM/);
  });

  it("la parte de fecha contiene el día de la semana y el mes", () => {
    const resultado = formatFecha("2024-06-15T10:30:00Z");
    expect(resultado).toMatch(/de \w+/);
  });
});

// ─── formatFechaLabel (Resumen del día) ────────────────────────────────────────

describe("formatFechaLabel", () => {
  it("formatea YYYY-MM-DD como dd/MM/yyyy", () => {
    expect(formatFechaLabel("2026-07-10")).toBe("10/07/2026");
  });

  it("conserva ceros a la izquierda en día y mes", () => {
    expect(formatFechaLabel("2026-01-05")).toBe("05/01/2026");
  });

  it("retorna cadena vacía para valores falsy", () => {
    expect(formatFechaLabel(null)).toBe("");
    expect(formatFechaLabel("")).toBe("");
    expect(formatFechaLabel(undefined)).toBe("");
  });
});

// ─── formatHora ──────────────────────────────────────────────────────────────

describe("formatHora", () => {
  it("retorna '—' para valores falsy", () => {
    expect(formatHora(null)).toBe("—");
    expect(formatHora("")).toBe("—");
    expect(formatHora(undefined)).toBe("—");
  });

  it("retorna una hora con AM o PM (hora12: true)", () => {
    const resultado = formatHora("2024-06-15T14:00:00Z");
    expect(resultado).toMatch(/AM|PM/);
  });

  it("no contiene el separador ' · ' (solo hora, sin fecha)", () => {
    const resultado = formatHora("2024-06-15T10:30:00Z");
    expect(resultado).not.toContain(" · ");
  });
});
