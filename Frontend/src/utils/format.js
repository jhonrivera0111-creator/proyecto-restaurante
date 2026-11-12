// ---------------------------------------------------------------------------
// utils/format.js — funciones de PRESENTACIÓN (dinero y fechas en formato colombiano).
// Regla del proyecto: nunca formatear dinero a mano con toLocaleString; usar
// formatCOP / formatUSD de este archivo para que todo se vea igual.
// ---------------------------------------------------------------------------

// Lee la zona horaria real del OS vía Intl (funciona incluso en Electron renderer)
const sysTZ = () => {
  try { return Intl.DateTimeFormat().resolvedOptions().timeZone; }
  catch { return undefined; }
};

/**
 * Formatea una fecha YYYY-MM-DD como dd/MM/yyyy para el módulo de Resumen del día.
 * Solo presentación — el estado interno de fecha sigue siendo YYYY-MM-DD
 * (query params, React Query keys, etc. no cambian).
 * Ejemplo: "2026-07-10" → "10/07/2026"
 */
export const formatFechaLabel = (fechaStr) => {
  if (!fechaStr) return "";
  const [y, m, d] = fechaStr.split("-");
  return `${d}/${m}/${y}`;
};

export const formatFecha = (isoString) => {
  if (!isoString) return "—";
  const d  = new Date(isoString);
  const tz = sysTZ();
  const weekday = d.toLocaleDateString("es-CO", { weekday: "long", timeZone: tz });
  const day     = d.toLocaleDateString("es-CO", { day: "numeric", timeZone: tz });
  const month   = d.toLocaleDateString("es-CO", { month: "long", timeZone: tz });
  const hora    = d.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: tz });
  const fecha   = `${weekday.charAt(0).toUpperCase() + weekday.slice(1)} ${day} de ${month}`;
  return `${fecha} · ${hora}`;
};

export const formatHora = (isoString) => {
  if (!isoString) return "—";
  const tz = sysTZ();
  return new Date(isoString).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: tz });
};

/**
 * Muestra la fecha/hora de creación de un pedido de forma contextual:
 * - Hoy          → "4:41 PM"
 * - Ayer         → "Ayer a las 4:41 PM"
 * - Este año     → "Miércoles 20 de mayo · 4:41 PM"
 * - Año anterior → "Miércoles 20 de mayo de 2024 · 4:41 PM"
 */
export const formatCreadoEn = (isoString) => {
  if (!isoString) return "—";
  const tz  = sysTZ();
  const d   = new Date(isoString);
  const hora = d.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true, timeZone: tz });

  // Fecha del evento y de hoy en zona local, como "YYYY-MM-DD"
  const toLocalDate = (date) =>
    date.toLocaleDateString("en-CA", { timeZone: tz }); // en-CA → YYYY-MM-DD

  const hoy = new Date();
  const ayer = new Date(hoy);
  ayer.setDate(ayer.getDate() - 1);

  const fechaEvento = toLocalDate(d);
  const fechaHoy    = toLocalDate(hoy);
  const fechaAyer   = toLocalDate(ayer);

  if (fechaEvento === fechaHoy)  return hora;
  if (fechaEvento === fechaAyer) return `Ayer a las ${hora}`;

  const mismoAnio = d.getFullYear() === hoy.getFullYear();
  const diaSemana = d.toLocaleDateString("es-CO", { weekday: "long", timeZone: tz });
  const dia       = d.toLocaleDateString("es-CO", { day: "numeric",  timeZone: tz });
  const mes       = d.toLocaleDateString("es-CO", { month: "long",   timeZone: tz });
  const anio      = d.getFullYear();

  const capFirst  = (s) => s.charAt(0).toUpperCase() + s.slice(1);
  const fechaStr  = mismoAnio
    ? `${capFirst(diaSemana)} ${dia} de ${mes}`
    : `${capFirst(diaSemana)} ${dia} de ${mes} de ${anio}`;

  return `${fechaStr} · ${hora}`;
};

export const formatCOP = (valor) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(valor ?? 0);

export const formatDecimal = (valor) =>
  typeof valor === "number" ? valor.toFixed(3).replace(/\.?0+$/, "") : "0";

export const formatUSD = (valor) =>
  `$${Number(valor ?? 0).toFixed(2)}`;
