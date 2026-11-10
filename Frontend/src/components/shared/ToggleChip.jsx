/** Chip toggle para campos opcionales (reserva, para llevar, salón, etc.) */
export default function ToggleChip({ label, active, onToggle }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className={`px-3 py-1 rounded-full text-xs font-semibold transition-colors cursor-pointer select-none
        ${active ? "bg-verde-claro text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"}`}
    >
      {label}
    </button>
  );
}
