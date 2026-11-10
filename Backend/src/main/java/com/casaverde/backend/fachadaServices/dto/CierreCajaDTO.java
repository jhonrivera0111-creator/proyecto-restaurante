package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CierreCajaDTO {
    private Long id;
    private LocalDate fecha;
    private Long usuarioId;
    private String usuarioNombre;
    private BigDecimal esperadoEfectivo;
    private BigDecimal esperadoUsd;
    private BigDecimal esperadoNequi;
    private BigDecimal contadoEfectivo;
    private BigDecimal contadoUsd;
    private BigDecimal contadoNequi;
    private BigDecimal diferenciaEfectivo;
    private BigDecimal diferenciaUsd;
    private BigDecimal diferenciaNequi;
    private BigDecimal cobradoTotalCop;
    private BigDecimal gastosTotalCop;
    private BigDecimal balanceNeto;
    private String observacion;
    private String backupPath;
    private LocalDateTime cerradoEn;

    // Denominaciones del arqueo (null en cierres históricos)
    private BigDecimal baseEfectivo;
    private Integer billetes100k;
    private Integer billetes50k;
    private Integer billetes20k;
    private Integer billetes10k;
    private Integer billetes5k;
    private Integer billetes2k;
    private BigDecimal monedasEfectivo;
    private BigDecimal paraEntregar;   // calculado: contadoEfectivo - baseEfectivo
}
