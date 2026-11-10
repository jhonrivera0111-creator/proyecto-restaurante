package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GastoDTO {
    private Long id;
    private String concepto;
    private String moneda;
    private BigDecimal monto;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String insumoInventario;
    private BigDecimal cantidadInventario;
    private String observacion;
    private LocalDateTime registradoEn;
}
