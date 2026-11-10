package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdelantoProveedorRequest {
    private Long proveedorId;
    private BigDecimal monto;
    private String moneda;
    private String insumo;
    private BigDecimal precioUnitarioAcordado;
    private LocalDate fecha;
    private String observacion;
}
