package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventarioDTO {
    private Long id;
    private String insumo;
    private BigDecimal stockActual;
    private BigDecimal alertaMinima;
    private String unidad;
    private LocalDateTime actualizadoEn;
}
