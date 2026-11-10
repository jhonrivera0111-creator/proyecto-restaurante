package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private Long id;
    private String moneda;
    private BigDecimal monto;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private LocalDateTime registradoEn;
}
