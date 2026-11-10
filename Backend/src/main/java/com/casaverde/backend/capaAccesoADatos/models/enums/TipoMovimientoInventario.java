package com.casaverde.backend.capaAccesoADatos.models.enums;

public enum TipoMovimientoInventario {
    ENTRADA, SALIDA, AJUSTE;

    public String valor() {
        return name().toLowerCase();
    }
}
