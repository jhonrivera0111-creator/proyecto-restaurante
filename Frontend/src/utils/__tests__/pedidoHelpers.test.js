import { describe, it, expect } from "vitest";
import {
  calcularInsumos,
  calcularInsumosDetallado,
  formatInsumo,
} from "../pedidoHelpers";

// ─── Fixtures ─────────────────────────────────────────────────────────────────

const detalleGallina1 = {
  id: 1,
  cantidad: 1,
  composicionSnapshot: JSON.stringify([{ insumo: "gallina", cantidad: 1 }]),
};

const detalleMediaGallina = {
  id: 2,
  cantidad: 1,
  composicionSnapshot: JSON.stringify([{ insumo: "gallina", cantidad: 0.5 }]),
};

const detalleCuy1 = {
  id: 3,
  cantidad: 1,
  composicionSnapshot: JSON.stringify([{ insumo: "cuy", cantidad: 1 }]),
};

// Almuerzo: un ítem con gallina Y cuy en su composición
const detalleAlmuerzo = {
  id: 4,
  cantidad: 1,
  composicionSnapshot: JSON.stringify([
    { insumo: "gallina", cantidad: 0.125 },
    { insumo: "cuy", cantidad: 0.5 },
  ]),
};

// ─── calcularInsumos ──────────────────────────────────────────────────────────

describe("calcularInsumos", () => {
  it("devuelve objeto vacío para array vacío", () => {
    expect(calcularInsumos([])).toEqual({});
  });

  it("devuelve objeto vacío si no se pasan argumentos", () => {
    expect(calcularInsumos()).toEqual({});
  });

  it("suma un insumo de un solo detalle", () => {
    expect(calcularInsumos([detalleGallina1])).toEqual({ gallina: 1 });
  });

  it("suma el mismo insumo en varios detalles", () => {
    expect(calcularInsumos([detalleGallina1, detalleGallina1])).toEqual({ gallina: 2 });
  });

  it("multiplica la composición por la cantidad del detalle", () => {
    const doble = { ...detalleGallina1, cantidad: 2 };
    expect(calcularInsumos([doble])).toEqual({ gallina: 2 });
  });

  it("acumula insumos fraccionarios correctamente", () => {
    const { gallina } = calcularInsumos([detalleGallina1, detalleMediaGallina]);
    expect(gallina).toBeCloseTo(1.5);
  });

  it("maneja ítems con múltiples insumos (almuerzo)", () => {
    const resultado = calcularInsumos([detalleAlmuerzo]);
    expect(resultado.gallina).toBeCloseTo(0.125);
    expect(resultado.cuy).toBeCloseTo(0.5);
  });

  it("omite detalles sin composicionSnapshot", () => {
    const sinComp = { id: 99, cantidad: 1, composicionSnapshot: null };
    expect(calcularInsumos([sinComp])).toEqual({});
  });

  it("omite detalles con JSON inválido (no lanza excepción)", () => {
    const malJson = { id: 99, cantidad: 1, composicionSnapshot: "no-es-json" };
    expect(() => calcularInsumos([malJson])).not.toThrow();
    expect(calcularInsumos([malJson])).toEqual({});
  });

  it("mezcla correctamente gallina y cuy de distintos detalles", () => {
    const resultado = calcularInsumos([detalleGallina1, detalleCuy1]);
    expect(resultado.gallina).toBe(1);
    expect(resultado.cuy).toBe(1);
  });
});

// ─── calcularInsumosDetallado ─────────────────────────────────────────────────

describe("calcularInsumosDetallado", () => {
  it("retorna las propiedades 'totales' y 'almuerzo'", () => {
    const resultado = calcularInsumosDetallado([]);
    expect(resultado).toHaveProperty("totales");
    expect(resultado).toHaveProperty("almuerzo");
  });

  it("array vacío → totales y almuerzo vacíos", () => {
    expect(calcularInsumosDetallado([])).toEqual({ totales: {}, almuerzo: {} });
  });

  it("ítem de gallina pura no conta en almuerzo", () => {
    const { totales, almuerzo } = calcularInsumosDetallado([detalleGallina1]);
    expect(totales.gallina).toBe(1);
    expect(almuerzo).toEqual({});
  });

  it("ítem de cuy puro no conta en almuerzo", () => {
    const { totales, almuerzo } = calcularInsumosDetallado([detalleCuy1]);
    expect(totales.cuy).toBe(1);
    expect(almuerzo).toEqual({});
  });

  it("almuerzo aparece en totales Y en almuerzo", () => {
    const { totales, almuerzo } = calcularInsumosDetallado([detalleAlmuerzo]);
    expect(totales.gallina).toBeCloseTo(0.125);
    expect(totales.cuy).toBeCloseTo(0.5);
    expect(almuerzo.gallina).toBeCloseTo(0.125);
    expect(almuerzo.cuy).toBeCloseTo(0.5);
  });

  it("totales agrupa gallina de almuerzo y gallina pura; almuerzo solo almuerzo", () => {
    const { totales, almuerzo } = calcularInsumosDetallado([
      detalleGallina1,
      detalleAlmuerzo,
    ]);
    expect(totales.gallina).toBeCloseTo(1.125);
    expect(almuerzo.gallina).toBeCloseTo(0.125); // solo la porción de almuerzo
  });
});

// ─── formatInsumo ────────────────────────────────────────────────────────────

describe("formatInsumo", () => {
  // Casos límite
  it("retorna null para total = 0", () => {
    expect(formatInsumo("gallina", 0)).toBeNull();
  });

  it("retorna null para total negativo", () => {
    expect(formatInsumo("gallina", -0.5)).toBeNull();
  });

  // Enteros
  it("1 gallina singular", () => {
    expect(formatInsumo("gallina", 1)).toBe("1 Gallina");
  });

  it("2 gallinas plural", () => {
    expect(formatInsumo("gallina", 2)).toBe("2 Gallinas");
  });

  it("1 cuy singular", () => {
    expect(formatInsumo("cuy", 1)).toBe("1 Cuy");
  });

  it("2 cuyes — plural irregular", () => {
    expect(formatInsumo("cuy", 2)).toBe("2 Cuyes");
  });

  // Fracciones menores a 1
  it("0.125 gallina → 1 presa", () => {
    expect(formatInsumo("gallina", 0.125)).toBe("1 Presa de gallina");
  });

  it("0.375 gallina → 3 presas", () => {
    expect(formatInsumo("gallina", 0.375)).toBe("3 Presas de gallina");
  });

  it("0.5 gallina → mitad", () => {
    expect(formatInsumo("gallina", 0.5)).toBe("Mitad de gallina");
  });

  it("0.625 gallina → mitad + 1 presa", () => {
    expect(formatInsumo("gallina", 0.625)).toBe("Mitad de gallina + 1 Presa de gallina");
  });

  it("0.25 cuy → 1 presa", () => {
    expect(formatInsumo("cuy", 0.25)).toBe("1 Presa de cuy");
  });

  it("0.5 cuy → mitad", () => {
    expect(formatInsumo("cuy", 0.5)).toBe("Mitad de cuy");
  });

  // Combinados (entero + fracción)
  it("1.5 gallina → 1 Gallina + Mitad de gallina", () => {
    expect(formatInsumo("gallina", 1.5)).toBe("1 Gallina + Mitad de gallina");
  });

  it("2.125 gallina → 2 Gallinas + 1 Presa de gallina", () => {
    expect(formatInsumo("gallina", 2.125)).toBe("2 Gallinas + 1 Presa de gallina");
  });

  it("1.25 cuy → 1 Cuy + 1 Presa de cuy", () => {
    expect(formatInsumo("cuy", 1.25)).toBe("1 Cuy + 1 Presa de cuy");
  });
});
