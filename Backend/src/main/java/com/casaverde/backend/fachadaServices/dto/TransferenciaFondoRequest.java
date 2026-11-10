package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferenciaFondoRequest {
    private String monedaOrigen;
    private BigDecimal montoOrigen;
    private String monedaDestino;
    private BigDecimal montoDestino;
    private String concepto;
    private String observacion;
    /** true = descontar GMF 4x1000 de NEQUI y registrarlo como gasto */
    private Boolean aplicar4x1000;
}
