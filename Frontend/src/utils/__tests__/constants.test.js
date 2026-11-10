import { describe, it, expect } from "vitest";
import { ESTADO_BADGE, CAT_BADGE } from "../constants";

// ─── ESTADO_BADGE ─────────────────────────────────────────────────────────────

describe("ESTADO_BADGE", () => {
  it("incluye todos los estados del pedido", () => {
    const estadosEsperados = ["PENDIENTE", "ABONADO_PARCIAL", "PAGADO", "FINALIZADO", "ANULADO"];
    estadosEsperados.forEach((estado) => {
      expect(ESTADO_BADGE).toHaveProperty(estado);
    });
  });

  it("no hay claves undefined o null", () => {
    Object.values(ESTADO_BADGE).forEach((clase) => {
      expect(clase).toBeTruthy();
      expect(typeof clase).toBe("string");
    });
  });

  it("los valores son clases CSS tipo badge-*", () => {
    Object.values(ESTADO_BADGE).forEach((clase) => {
      expect(clase).toMatch(/^badge-/);
    });
  });

  it("PENDIENTE usa badge-gray (neutro)", () => {
    expect(ESTADO_BADGE.PENDIENTE).toBe("badge-gray");
  });

  it("ABONADO_PARCIAL usa badge-warning (intermedio)", () => {
    expect(ESTADO_BADGE.ABONADO_PARCIAL).toBe("badge-warning");
  });

  it("PAGADO usa badge-success", () => {
    expect(ESTADO_BADGE.PAGADO).toBe("badge-success");
  });

  it("FINALIZADO usa badge-info", () => {
    expect(ESTADO_BADGE.FINALIZADO).toBe("badge-info");
  });

  it("ANULADO usa badge-danger", () => {
    expect(ESTADO_BADGE.ANULADO).toBe("badge-danger");
  });
});

// ─── CAT_BADGE ────────────────────────────────────────────────────────────────

describe("CAT_BADGE", () => {
  it("incluye todas las categorías de gastos", () => {
    const categoriasEsperadas = ["insumos", "servicios", "equipos", "otros"];
    categoriasEsperadas.forEach((cat) => {
      expect(CAT_BADGE).toHaveProperty(cat);
    });
  });

  it("no hay claves undefined o null", () => {
    Object.values(CAT_BADGE).forEach((clase) => {
      expect(clase).toBeTruthy();
      expect(typeof clase).toBe("string");
    });
  });

  it("los valores son clases CSS tipo badge-*", () => {
    Object.values(CAT_BADGE).forEach((clase) => {
      expect(clase).toMatch(/^badge-/);
    });
  });

  it("insumos usa badge-success (recursos productivos)", () => {
    expect(CAT_BADGE.insumos).toBe("badge-success");
  });

  it("servicios usa badge-warning", () => {
    expect(CAT_BADGE.servicios).toBe("badge-warning");
  });
});
