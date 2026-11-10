package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventarioDTO {
    private Long id;
    private String insumo;
    private String tipo;
    private BigDecimal cantidad;
    private BigDecimal stockResultante;
    private String referenciaTipo;
    private Long referenciaId;
    private String observacion;
    private LocalDateTime registradoEn;
}
