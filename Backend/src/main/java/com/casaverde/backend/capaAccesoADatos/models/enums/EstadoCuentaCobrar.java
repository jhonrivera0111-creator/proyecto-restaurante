package com.casaverde.backend.capaAccesoADatos.models.enums;

import java.math.BigDecimal;

public enum EstadoCuentaCobrar {
    PENDIENTE, ABONADO_PARCIAL, CANCELADO;

    public static EstadoCuentaCobrar calcular(BigDecimal monto, BigDecimal abonado) {
        if (abonado == null || abonado.compareTo(BigDecimal.ZERO) <= 0)
            return PENDIENTE;
        if (abonado.compareTo(monto) >= 0)
            return CANCELADO;
        return ABONADO_PARCIAL;
    }
}
