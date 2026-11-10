package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoPrecioHistorialDTO {
    private Long id;
    private Long productoId;
    private String productoNombre;
    private BigDecimal precioAnterior;
    private BigDecimal precioNuevo;
    private String motivo;
    private Long usuarioId;
    private LocalDateTime registradoEn;
}
