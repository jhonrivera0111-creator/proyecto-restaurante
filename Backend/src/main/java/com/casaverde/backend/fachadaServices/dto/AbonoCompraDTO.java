package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCompraDTO {
    private Long id;
    private Long compraId;
    private BigDecimal monto;
    private String moneda;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String fondoDebitado;
    private Long adelantoProveedorId;
    private LocalDate fecha;
    private LocalDateTime creadoEn;
}
