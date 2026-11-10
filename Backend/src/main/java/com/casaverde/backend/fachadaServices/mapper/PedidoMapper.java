package com.casaverde.backend.fachadaServices.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDetalleDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoTotales;

/**
 * Convierte entidades de Pedido a DTOs. Los cálculos financieros (totales, abonado)
 * son responsabilidad de PedidoServiceImpl.calcularTotales() y se pasan como PedidoTotales.
 * Este mapper permanece como @Component (no MapStruct) porque recibe ese parámetro extra.
 */
@Component
public class PedidoMapper {

    public PedidoDTO toDTO(Pedido p, PedidoTotales totales) {
        return PedidoDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .salonId(p.getSalon() != null ? p.getSalon().getId() : null)
                .salonNombre(p.getSalon() != null ? p.getSalon().getNombre() : null)
                .mesa(p.getMesa())
                .fechaReserva(p.getFechaReserva() != null ? p.getFechaReserva().toString() : null)
                .horaReserva(p.getHoraReserva() != null ? p.getHoraReserva().toString() : null)
                .cantPersonas(p.getCantPersonas())
                .estado(p.getEstado() != null ? p.getEstado().name() : null)
                .observacion(p.getObservacion())
                .paraLlevar(Boolean.TRUE.equals(p.getParaLlevar()))
                .telefono(p.getTelefono())
                .enCurso(calcularEnCurso(p))
                .subtotalCop(totales.subtotalCop())
                .descuentoCop(p.getDescuentoCop())
                .descuentoAplicadoPor(p.getDescuentoAplicadoPor())
                .totalCop(totales.totalCop())
                .totalAbonado(totales.totalAbonado())
                .saldoPendiente(totales.saldoPendiente())
                .valorCambio(totales.valorCambio())
                .creadoEn(p.getCreadoEn())
                .actualizadoEn(p.getActualizadoEn())
                .finalizadoEn(p.getFinalizadoEn())
                .fiadoEn(p.getFiadoEn())
                .detalles(p.getDetalles().stream().map(this::toDetalleDTO).collect(Collectors.toList()))
                .pagos(p.getPagos().stream().map(this::toPagoDTO).collect(Collectors.toList()))
                .build();
    }

    private boolean calcularEnCurso(Pedido p) {
        if (p.getEstado() == null || !p.getEstado().esActivo()) return false;
        if (p.getFechaReserva() == null && p.getHoraReserva() == null) return true;

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Bogota"));
        LocalDate today = now.toLocalDate();
        LocalDate rFecha = p.getFechaReserva() != null ? p.getFechaReserva() : today;

        if (rFecha.isBefore(today)) return true;
        if (rFecha.isAfter(today))  return false;
        // Es hoy
        if (p.getHoraReserva() == null) return true;
        return !p.getHoraReserva().isAfter(now.toLocalTime());
    }

    public PedidoDetalleDTO toDetalleDTO(PedidoDetalle d) {
        return PedidoDetalleDTO.builder()
                .id(d.getId())
                .productoId(d.getProducto() != null ? d.getProducto().getId() : null)
                .nombreSnapshot(d.getNombreSnapshot())
                .precioUnitarioCop(d.getPrecioUnitarioCop())
                .cantidad(d.getCantidad())
                .subtotalCop(d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                .composicionSnapshot(d.getComposicionSnapshot())
                .build();
    }

    public PagoDTO toPagoDTO(Pago p) {
        return PagoDTO.builder()
                .id(p.getId())
                .moneda(p.getMoneda())
                .monto(p.getMonto())
                .montoCop(p.getMontoCop())
                .tasaUsada(p.getTasaUsada())
                .registradoEn(p.getRegistradoEn())
                .build();
    }
}
