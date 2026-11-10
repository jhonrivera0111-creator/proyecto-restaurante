/**
 * Input de dinero con formato de miles (COP/NEQUI) o decimal (USD).
 * - COP/NEQUI: muestra separadores de miles, almacena solo dígitos.
 *   ↑/↓ ajusta en pasos de $1.000.
 * - USD: input numérico estándar con step=0.01.
 */
export default function MoneyInput({ value, onChange, moneda = "COP", placeholder, className, autoFocus, id, onBlur, readOnly }) {
  const isCop = moneda !== "USD";

  const toDisplay = (raw) => {
    if (raw === "" || raw == null) return "";
    const digits = String(raw).replace(/\D/g, "");
    if (!digits) return "";
    const n = parseInt(digits, 10);
    return isNaN(n) ? "" : n.toLocaleString("es-CO"); // 100000 → "100.000"
  };

  const handleChange = (e) => {
    if (readOnly || !onChange) return;
    if (!isCop) { onChange(e.target.value); return; }
    onChange(e.target.value.replace(/\D/g, ""));
  };

  const handleKeyDown = (e) => {
    if (readOnly || !onChange) return;
    if (!isCop || (e.key !== "ArrowUp" && e.key !== "ArrowDown")) return;
    e.preventDefault();
    const cur = parseInt(String(value).replace(/\D/g, "") || "0", 10);
    onChange(String(e.key === "ArrowUp" ? cur + 1000 : Math.max(0, cur - 1000)));
  };

  return (
    <input
      type={isCop ? "text" : "number"}
      inputMode={isCop ? "numeric" : "decimal"}
      className={className}
      placeholder={placeholder}
      value={isCop ? toDisplay(value) : value}
      onChange={handleChange}
      onKeyDown={handleKeyDown}
      autoFocus={autoFocus}
      id={id}
      onBlur={onBlur}
      readOnly={readOnly}
      {...(!isCop && { step: "0.01", min: "0" })}
    />
  );
}
