package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;

/**
 * Totales calculados de un pedido. Producido por PedidoServiceImpl.calcularTotales()
 * y consumido por PedidoMapper.toDTO() para mantener el mapper libre de aritmética.
 */
public record PedidoTotales(
        BigDecimal subtotalCop,
        BigDecimal totalCop,
        BigDecimal totalAbonado,
        BigDecimal saldoPendiente,
        BigDecimal valorCambio
) {
    /**
     * totalCop = subtotal - descuento, sin clamp: PedidoServiceImpl garantiza en
     * cada punto de escritura (crear, agregar/actualizar/quitar detalle) que
     * 0 <= descuentoCop < subtotalCop, así que la resta siempre es positiva. No
     * se usa max(..., 0) aquí — eso ocultaría un descuento inválido en vez de
     * rechazarlo donde ocurre. El descuento no toca el precioUnitarioCop
     * histórico de cada detalle — se resta solo a nivel de pedido.
     */
    public static PedidoTotales of(Pedido pedido) {
        BigDecimal subtotal = pedido.getDetalles().stream()
                .map(d -> d.getPrecioUnitarioCop().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal descuento = pedido.getDescuentoCop() != null ? pedido.getDescuentoCop() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(descuento);
        BigDecimal abonado = pedido.getPagos().stream()
                .map(Pago::getMontoCop)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PedidoTotales(
                subtotal,
                total,
                abonado,
                total.subtract(abonado).max(BigDecimal.ZERO),
                abonado.subtract(total).max(BigDecimal.ZERO));
    }
}
