package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaRequest {
    private LocalDate fecha;
    private Long usuarioId;
    private BigDecimal contadoEfectivo;
    private BigDecimal contadoUsd;
    private BigDecimal contadoNequi;
    private String observacion;

    // Denominaciones opcionales — si se envían, contadoEfectivo se calcula desde ellas
    private BigDecimal baseEfectivo;
    private Integer billetes100k;
    private Integer billetes50k;
    private Integer billetes20k;
    private Integer billetes10k;
    private Integer billetes5k;
    private Integer billetes2k;
    private BigDecimal monedasEfectivo;
}
