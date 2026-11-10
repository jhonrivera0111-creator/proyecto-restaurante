package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDetalleDTO;

@Service
public class ComprobanteServiceImpl implements IComprobanteService {

    @Override
    public String generar(PedidoDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("ASADERO CASA VERDE").append(System.lineSeparator());
        sb.append("Comprobante pedido #").append(dto.getId()).append(System.lineSeparator());
        sb.append("--------------------------------").append(System.lineSeparator());
        sb.append("Cliente: ").append(dto.getNombre()).append(System.lineSeparator());
        if (dto.getTelefono() != null) {
            sb.append("Telefono: ").append(dto.getTelefono()).append(System.lineSeparator());
        }
        sb.append("Estado: ").append(dto.getEstado()).append(System.lineSeparator());
        if (dto.getSalonNombre() != null || dto.getMesa() != null) {
            sb.append("Ubicacion: ")
                    .append(dto.getSalonNombre() != null ? dto.getSalonNombre() : "")
                    .append(dto.getMesa() != null ? " / Mesa " + dto.getMesa() : "")
                    .append(System.lineSeparator());
        }
        if (dto.getFinalizadoEn() != null) {
            sb.append("Cierre: ")
                    .append(dto.getFinalizadoEn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append(System.lineSeparator());
        }
        sb.append("--------------------------------").append(System.lineSeparator());
        for (PedidoDetalleDTO detalle : dto.getDetalles()) {
            sb.append(detalle.getCantidad()).append(" x ")
                    .append(detalle.getNombreSnapshot())
                    .append("  COP ").append(detalle.getSubtotalCop())
                    .append(System.lineSeparator());
        }
        sb.append("--------------------------------").append(System.lineSeparator());
        if (dto.getDescuentoCop() != null && dto.getDescuentoCop().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Subtotal COP: ").append(dto.getSubtotalCop()).append(System.lineSeparator());
            sb.append("Descuento COP: -").append(dto.getDescuentoCop());
            if (dto.getDescuentoAplicadoPor() != null) {
                sb.append(" (").append(dto.getDescuentoAplicadoPor()).append(")");
            }
            sb.append(System.lineSeparator());
        }
        sb.append("Total COP: ").append(dto.getTotalCop()).append(System.lineSeparator());
        sb.append("Abonado COP: ").append(dto.getTotalAbonado()).append(System.lineSeparator());
        if (dto.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Saldo pendiente COP: ").append(dto.getSaldoPendiente()).append(System.lineSeparator());
        }
        if (dto.getValorCambio().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Cambio COP: ").append(dto.getValorCambio()).append(System.lineSeparator());
        }
        if (!dto.getPagos().isEmpty()) {
            sb.append("--------------------------------").append(System.lineSeparator());
            sb.append("Pagos").append(System.lineSeparator());
            for (PagoDTO pago : dto.getPagos()) {
                sb.append(pago.getMoneda()).append(" ")
                        .append(pago.getMonto())
                        .append(" = COP ").append(pago.getMontoCop());
                if (pago.getTasaUsada() != null) {
                    sb.append(" tasa ").append(pago.getTasaUsada());
                }
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }
}
