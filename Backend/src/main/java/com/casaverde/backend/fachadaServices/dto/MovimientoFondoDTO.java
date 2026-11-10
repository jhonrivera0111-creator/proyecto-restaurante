package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoFondoDTO {
    private Long id;
    private String tipo; // "cobro" | "gasto"
    private String moneda;
    private BigDecimal monto;
    private BigDecimal montoCop;
    private BigDecimal saldoResultante;
    private String concepto;
    private String referenciaTipo;
    private Long referenciaId;
    private String observacion;
    private LocalDateTime fecha;
}
