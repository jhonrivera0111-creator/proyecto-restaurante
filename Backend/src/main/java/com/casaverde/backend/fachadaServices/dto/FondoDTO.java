package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FondoDTO {
    private String moneda;
    private String alias;
    private BigDecimal saldo;
    private LocalDateTime actualizadoEn;
}
