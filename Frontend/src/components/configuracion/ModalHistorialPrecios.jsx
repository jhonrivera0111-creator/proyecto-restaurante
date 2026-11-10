import { X } from "lucide-react";
import { formatCOP } from "../../utils/format";
import { useModalKeyboard } from "../../hooks/useModalKeyboard";

/** Modal de historial de cambios de precio de un producto. Extraído de ProductosTab. */
export default function ModalHistorialPrecios({ producto, historial, loading, onClose }) {
  useModalKeyboard({ onEscape: onClose });
  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <span className="modal-title">Historial de precios - {producto.nombre}</span>
          <button className="btn-icon" onClick={onClose}><X size={18} /></button>
        </div>
        <div className="modal-body">
          {loading ? (
            <div className="loading-center"><div className="spinner" /></div>
          ) : historial.length === 0 ? (
            <div className="empty-state"><p>Sin cambios de precio</p></div>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Anterior</th>
                  <th>Nuevo</th>
                  <th>Motivo</th>
                </tr>
              </thead>
              <tbody>
                {historial.map((h) => (
                  <tr key={h.id}>
                    <td className="text-[12px] text-gray-500">
                      {new Date(h.registradoEn).toLocaleString("es-CO")}
                    </td>
                    <td>{h.precioAnterior == null ? "-" : formatCOP(h.precioAnterior)}</td>
                    <td className="font-semibold">{formatCOP(h.precioNuevo)}</td>
                    <td><span className="badge badge-gray">{h.motivo}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
