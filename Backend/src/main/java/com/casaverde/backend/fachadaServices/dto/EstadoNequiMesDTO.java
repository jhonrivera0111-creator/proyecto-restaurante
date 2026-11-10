package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoNequiMesDTO {
    /** Total retirado de NEQUI en el mes en curso (transferencias + gastos). */
    private BigDecimal retiroAcumulado;
    /** Límite mensual exento de 4x1000 configurado en el sistema. */
    private BigDecimal limiteExento;
    /** Monto que aún puede retirarse sin pagar 4x1000 (0 si ya se superó). */
    private BigDecimal disponibleLibre;
    /** Mes en curso, formato "yyyy-MM". */
    private String mes;
}
