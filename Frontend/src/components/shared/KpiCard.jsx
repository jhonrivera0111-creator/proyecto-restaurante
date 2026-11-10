// color → Tailwind border-l + text classes
const COLOR_CLS = {
  "var(--warning)":        { border: "border-l-amber-500",   text: "text-amber-600" },
  "var(--danger)":         { border: "border-l-red-600",     text: "text-red-600" },
  "var(--verde-claro)":    { border: "border-l-verde-claro", text: "text-verde-claro" },
  "var(--text-secondary)": { border: "border-l-gray-300",    text: "text-gray-400" },
};

/**
 * Tarjeta KPI reutilizable.
 * @param {string}    label  - Etiqueta superior
 * @param {*}         value  - Valor principal
 * @param {string}   [sub]   - Texto secundario debajo del valor
 * @param {string}   [color] - Token CSS de color (ej. "var(--verde-claro)")
 * @param {Component}[icon]  - Componente ícono de lucide-react (se muestra arriba-derecha)
 * @param {boolean}  [dim]   - Aplica opacity-50 a la tarjeta
 */
export default function KpiCard({ label, value, sub, color, icon: Icon, dim }) {
  const cls = COLOR_CLS[color] ?? {};
  return (
    <div className={`kpi-card ${cls.border ?? ""} ${dim ? "opacity-50" : ""}`}>
      <div className="flex justify-between items-start">
        <div>
          <div className="kpi-label">{label}</div>
          <div className={`kpi-value ${cls.text ?? ""}`}>{value}</div>
          {sub && <div className="text-[12px] text-gray-500 mt-0.5">{sub}</div>}
        </div>
        {Icon && <Icon size={22} className={`${cls.text ?? "text-verde-claro"} opacity-60`} />}
      </div>
    </div>
  );
}
