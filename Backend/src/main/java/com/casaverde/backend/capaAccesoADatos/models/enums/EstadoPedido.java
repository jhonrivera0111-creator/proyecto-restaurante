package com.casaverde.backend.capaAccesoADatos.models.enums;

import java.math.BigDecimal;
import java.util.List;

public enum EstadoPedido {
    PENDIENTE,
    ABONADO_PARCIAL,
    PAGADO,
    FIADO,
    FINALIZADO,
    ANULADO;

    public static EstadoPedido from(String value) {
        if (value == null || value.isBlank() || "ABIERTO".equalsIgnoreCase(value)) {
            return PENDIENTE;
        }
        return EstadoPedido.valueOf(value.trim().toUpperCase());
    }

    /**
     * Permite editar ítems y datos generales del pedido. FIADO queda excluido
     * a propósito: una vez fiado el pedido se congela (no se agregan/quitan
     * ítems) — solo admite abonos hasta saldarse.
     */
    public boolean permiteEdicion() {
        return this != FIADO && this != FINALIZADO && this != ANULADO;
    }

    /**
     * Permite registrar/editar/quitar pagos. A diferencia de permiteEdicion(),
     * SÍ incluye FIADO — un pedido fiado sigue recibiendo abonos hasta
     * saldarse, aunque sus ítems ya no se puedan tocar.
     */
    public boolean permiteRegistrarPago() {
        return this == PENDIENTE || this == ABONADO_PARCIAL || this == PAGADO || this == FIADO;
    }

    /**
     * Pedidos del flujo activo normal de Pedidos (tab Activos, Dashboard,
     * bloqueo de cierre de caja). FIADO se excluye deliberadamente: una vez
     * fiado, el pedido se gestiona desde Cuentas por Cobrar y no debe
     * bloquear el cierre de caja del día.
     */
    public boolean esActivo() {
        return this == PENDIENTE || this == ABONADO_PARCIAL || this == PAGADO;
    }

    public static List<EstadoPedido> activos() {
        return List.of(PENDIENTE, ABONADO_PARCIAL, PAGADO);
    }

    public static EstadoPedido calcular(BigDecimal total, BigDecimal abonado) {
        if (total.compareTo(BigDecimal.ZERO) <= 0 || abonado.compareTo(BigDecimal.ZERO) <= 0)
            return PENDIENTE;
        if (abonado.compareTo(total) < 0)
            return ABONADO_PARCIAL;
        return PAGADO;
    }
}
