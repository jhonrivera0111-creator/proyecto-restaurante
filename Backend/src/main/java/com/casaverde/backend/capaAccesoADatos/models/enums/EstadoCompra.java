package com.casaverde.backend.capaAccesoADatos.models.enums;

import java.math.BigDecimal;

public enum EstadoCompra {
    PENDIENTE, ABONADO_PARCIAL, PAGADO;

    public static EstadoCompra calcular(BigDecimal total, BigDecimal abonado) {
        if (abonado == null || abonado.compareTo(BigDecimal.ZERO) <= 0)
            return PENDIENTE;
        if (abonado.compareTo(total) >= 0)
            return PAGADO;
        return ABONADO_PARCIAL;
    }
}
