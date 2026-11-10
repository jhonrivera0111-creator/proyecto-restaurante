// Helpers compartidos para cálculo y formateo de insumos de pedidos.
// Los datos de composición vienen del snapshot guardado en cada DetallePedido;
// estas funciones solo los parsean para display — no recalculan lógica de negocio.

/**
 * Suma los insumos requeridos para una lista de detalles de pedido.
 * @param {Array} detalles
 * @returns {Object} { gallina: number, cuy: number, ... }
 */
export function calcularInsumos(detalles = []) {
  const totales = {};
  for (const d of detalles) {
    if (!d.composicionSnapshot) continue;
    try {
      for (const { insumo, cantidad } of JSON.parse(d.composicionSnapshot)) {
        totales[insumo] = (totales[insumo] ?? 0) + cantidad * d.cantidad;
      }
    } catch {
      /* empty */
    }
  }
  return totales;
}

/**
 * Como calcularInsumos, pero también separa cuánto corresponde a ítems de "almuerzo"
 * (ítems que contienen tanto gallina como cuy en su composición).
 * @param {Array} detalles
 * @returns {{ totales: Object, almuerzo: Object }}
 */
export function calcularInsumosDetallado(detalles = []) {
  const totales = {};
  const almuerzo = {};
  for (const d of detalles) {
    if (!d.composicionSnapshot) continue;
    try {
      const comp = JSON.parse(d.composicionSnapshot);
      const esAlmuerzo =
        comp.some((c) => c.insumo === "gallina") &&
        comp.some((c) => c.insumo === "cuy");
      for (const { insumo, cantidad } of comp) {
        totales[insumo] = (totales[insumo] ?? 0) + cantidad * d.cantidad;
        if (esAlmuerzo)
          almuerzo[insumo] = (almuerzo[insumo] ?? 0) + cantidad * d.cantidad;
      }
    } catch {
      /* empty */
    }
  }
  return { totales, almuerzo };
}

// Unidad mínima por insumo (= 1 presa)
const INSUMO_UNIDAD = { gallina: 0.125, cuy: 0.25 };

// Capitaliza el primer carácter alfabético
export const capFirst = (s) =>
  s.replace(/[a-záéíóúñ]/i, (c) => c.toUpperCase());

// Devuelve partes textuales para el fraccionario n (ej. 0.375 → ["Mitad de gallina", "2 presas de gallina"])
function fracParts(insumo, n) {
  const unidad = INSUMO_UNIDAD[insumo];
  if (n >= 0.5 - 0.001) {
    const resto = Math.round((n - 0.5) * 1000) / 1000;
    const mitad = `mitad de ${insumo}`;
    if (resto < 0.001) return [mitad];
    if (!unidad) return [mitad, `${parseFloat(resto.toFixed(3))} ${insumo}`];
    const presas = Math.round(resto / unidad);
    if (presas === 0) return [mitad];
    return [
      mitad,
      `${presas} ${presas === 1 ? "presa" : "presas"} de ${insumo}`,
    ];
  }
  if (!unidad) return [`${parseFloat(n.toFixed(3))} ${insumo}`];
  const presas = Math.round(n / unidad);
  return [`${presas} ${presas === 1 ? "presa" : "presas"} de ${insumo}`];
}

/**
 * Formatea una cantidad de insumo en texto legible.
 * Ejemplos: 1.5 gallina → "1 Gallina + Mitad De Gallina"
 * @param {string} insumo
 * @param {number} total
 * @returns {string|null}
 */
export function formatInsumo(insumo, total) {
  if (total <= 0) return null;
  const plural = insumo === "cuy" ? "cuyes" : `${insumo}s`;
  const intPart = Math.floor(total);
  const fracPart = Math.round((total - intPart) * 1000) / 1000;
  const parts = [];
  if (intPart > 0) parts.push(`${intPart} ${intPart === 1 ? insumo : plural}`);
  if (fracPart > 0.001) parts.push(...fracParts(insumo, fracPart));
  return parts.map(capFirst).join(" + ");
}

// ── Pedidos activos (no finalizados ni anulados) ──────────────────────────────
export const ESTADOS_ACTIVOS = new Set([
  "PENDIENTE",
  "ABONADO_PARCIAL",
  "PAGADO",
]);
export const esActivo = (estado) => ESTADOS_ACTIVOS.has(estado);

// FIADO se excluye a propósito de esActivo(): un pedido fiado ya no cuenta
// como "pedido activo" en el módulo de Pedidos (ítems congelados, no
// aparece en la pestaña Activos). Pero SÍ puede seguir recibiendo abonos y
// saldarse — permiteCobro() es lo que usan OrdenCard/ModalCobrar/
// ModalDetallePedido para mostrar esas acciones también quedan disponibles
// cuando ese pedido se reutiliza dentro de Cuentas por Cobrar.
export const ESTADOS_CON_COBRO = new Set([...ESTADOS_ACTIVOS, "FIADO"]);
export const permiteCobro = (estado) => ESTADOS_CON_COBRO.has(estado);

// ── Normalización de fracciones ───────────────────────────────────────────────
/** Reglas de cascada: 2×½Cuy → 1×Cuy, 4×⅛Gallina → 1×½Gallina, etc. */
export const REGLAS_FRACCION = [
  { from: "1/8 Gallina", fromQty: 4, to: "1/2 Gallina", toQty: 1 },
  { from: "1/2 Gallina", fromQty: 2, to: "Gallina", toQty: 1 },
  { from: "1/4 Cuy", fromQty: 4, to: "Cuy", toQty: 1 },
  { from: "1/2 Cuy", fromQty: 2, to: "Cuy", toQty: 1 },
];

/**
 * Normaliza las cantidades del carrito aplicando las reglas de fracción en cascada.
 * @param {Object} cantidades - map { productoId: cantidad }
 * @param {Array}  productos  - lista completa con { id, nombre }
 */
export function normalizarFracciones(cantidades, productos) {
  const findId = (nombre) =>
    productos.find(
      (p) => p.nombre.trim().toLowerCase() === nombre.toLowerCase(),
    )?.id ?? null;
  let cur = { ...cantidades };
  let changed = true;
  while (changed) {
    changed = false;
    for (const { from, fromQty, to, toQty } of REGLAS_FRACCION) {
      const fromId = findId(from);
      const toId = findId(to);
      if (!fromId || !toId) continue;
      const qty = cur[fromId] ?? 0;
      if (qty >= fromQty) {
        const times = Math.floor(qty / fromQty);
        const remainder = qty % fromQty;
        cur = {
          ...cur,
          [fromId]: remainder,
          [toId]: (cur[toId] ?? 0) + times * toQty,
        };
        if (remainder === 0) {
          const { [fromId]: _, ...rest } = cur;
          cur = rest;
        }
        changed = true;
      }
    }
  }
  return cur;
}

// ── Capitaliza cada palabra (title case) ─────────────────────────────────────
// \b no sirve aquí: en JS \b se define sobre \w (solo ASCII), así que trata
// á/é/í/ó/ú/ü/ñ como "no palabra" y genera un falso límite de palabra a cada
// lado de esas letras (ej. "Peña" → "PeÑA"). En vez de \b, se ancla a
// inicio-de-string o cualquier carácter que NO sea letra (espacio, guión…).
export const toTitleCase = (s) =>
  s.replace(/(^|[^a-záéíóúüñ])([a-záéíóúüñ])/gi, (_, sep, c) => sep + c.toUpperCase());

// ── Formateo de fechas y horas de reserva ────────────────────────────────────

/** "2026-05-23" → "sábado 23 de mayo" (sin año si es el año actual) */
export function formatFechaReserva(fechaStr) {
  if (!fechaStr) return null;
  const [y, m, d] = fechaStr.split("-").map(Number);
  const date = new Date(y, m - 1, d);
  const mismoAnio = y === new Date().getFullYear();
  const diaSemana = capFirst(
    date.toLocaleDateString("es-CO", { weekday: "long" }),
  );
  const mes = date.toLocaleDateString("es-CO", { month: "long" });
  return mismoAnio
    ? `${diaSemana} ${d} de ${mes}`
    : `${diaSemana} ${d} de ${mes} de ${y}`;
}

/** "07:00" → "7:00 AM" · "15:07" → "3:07 PM" */
export function formatHoraReserva(h) {
  if (!h) return null;
  const [hh, mm] = h.split(":").map(Number);
  const period = hh >= 12 ? "PM" : "AM";
  const h12 = hh % 12 || 12;
  return `${h12}:${String(mm).padStart(2, "0")} ${period}`;
}

/** Muestra la reserva contextualmente: "Hoy a las 7:00 PM", "Sábado 24 de mayo · 7:00 PM", etc. */
export function formatReservaDisplay(fechaStr, horaStr) {
  if (!fechaStr && !horaStr) return null;
  const hoy = new Date().toLocaleDateString("en-CA");
  if (!fechaStr || fechaStr === hoy) {
    return horaStr ? `Hoy a las ${formatHoraReserva(horaStr)}` : "Hoy";
  }
  const fecha = formatFechaReserva(fechaStr);
  const hora = horaStr ? formatHoraReserva(horaStr) : null;
  return [fecha, hora].filter(Boolean).join(" · ");
}


export const formatFraccion = (cantidad) => {
  if (cantidad === 1)      return "1";
  if (cantidad === 0.5)    return "½";
  if (cantidad === 0.25)   return "¼";
  if (cantidad === 0.125)  return "⅛";
  if (cantidad === 0.75)   return "¾";
  if (cantidad === 0.375)  return "⅜";
  if (cantidad === 0.625)  return "⅝";
  if (cantidad === 0.875)  return "⅞";
  return String(cantidad);
};

// ── Ordenación de pedidos por reserva ────────────────────────────────────────
/**
 * Grupo A (aparecen primero, orden cronológico asc):
 *   - Sin reserva → tiempo efectivo = creadoEn
 *   - Reserva vencida (hora ya pasó) → tiempo efectivo = hora de reserva
 * Grupo B (al final, del más próximo al más lejano):
 *   - Reserva futura
 *
 * Ejemplo: reserva 9:00 (vencida) → walk-in 9:10 → reserva 10:00 (futura)
 */
export function sortByReserva(list) {
  const now   = new Date();
  const today = now.toLocaleDateString("en-CA");

  const resTs = (p) => {
    if (!p.fechaReserva && !p.horaReserva) return null;
    if (p.fechaReserva && p.horaReserva)
      return new Date(`${p.fechaReserva}T${p.horaReserva}`).getTime();
    if (p.fechaReserva)
      return new Date(`${p.fechaReserva}T00:00`).getTime();
    return new Date(`${today}T${p.horaReserva}`).getTime();
  };

  const isFuture = (p) => { const t = resTs(p); return t !== null && t > now.getTime(); };

  const effectiveTs = (p) => {
    const t = resTs(p);
    return t !== null && t <= now.getTime() ? t : new Date(p.creadoEn).getTime();
  };

  return [...list].sort((a, b) => {
    const af = isFuture(a), bf = isFuture(b);
    if (af && bf)  return resTs(a) - resTs(b); // ambas futuras: más próxima primero
    if (af)        return 1;                   // a futura → va al final
    if (bf)        return -1;                  // b futura → va al final
    return effectiveTs(a) - effectiveTs(b);    // grupo A: cronológico asc
  });
}
