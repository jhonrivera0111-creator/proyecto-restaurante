package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AbonoCuentaCobrarDTO {
    private Long id;
    private Long cuentaId;
    private BigDecimal monto;
    private String moneda;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String fondoAcreditado;
    private LocalDate fecha;
    private String observacion;
    private LocalDateTime creadoEn;
}
