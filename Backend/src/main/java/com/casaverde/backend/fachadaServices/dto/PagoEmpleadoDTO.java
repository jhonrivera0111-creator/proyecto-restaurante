package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoPagoEmpleado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoEmpleadoDTO {
    private Long id;
    private Long empleadoId;
    private TipoPagoEmpleado tipo;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private Integer diasTrabajados;
    private BigDecimal salarioBruto;
    private BigDecimal totalAdelantos;
    private BigDecimal totalNeto;
    private String moneda;
    private BigDecimal montoPagado;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String fondoDebitado;
    private String observacion;
    private LocalDateTime creadoEn;
}
