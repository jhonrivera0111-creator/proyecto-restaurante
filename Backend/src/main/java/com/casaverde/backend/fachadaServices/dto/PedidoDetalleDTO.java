package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDetalleDTO {
    private Long id;
    private Long productoId;
    private String nombreSnapshot;
    private BigDecimal precioUnitarioCop;
    private Integer cantidad;
    private BigDecimal subtotalCop;
    private String composicionSnapshot;
}
