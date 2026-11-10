package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntradaFondoRequest {
    private String moneda;
    private BigDecimal monto;
    private String concepto;
    private String observacion;
}
