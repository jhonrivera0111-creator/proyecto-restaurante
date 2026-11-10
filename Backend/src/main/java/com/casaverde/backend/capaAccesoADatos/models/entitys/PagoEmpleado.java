package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoPagoEmpleado;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pago_empleado", indexes = {
        @Index(name = "idx_pago_empleado_empleado", columnList = "empleado_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoPagoEmpleado tipo;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fin", nullable = false)
    private LocalDate periodoFin;

    @Column(name = "dias_trabajados")
    private Integer diasTrabajados;

    @Column(name = "salario_bruto", nullable = false, precision = 19, scale = 0)
    private BigDecimal salarioBruto;

    @Column(name = "total_adelantos", nullable = false, precision = 19, scale = 0)
    private BigDecimal totalAdelantos;

    @Column(name = "total_neto", nullable = false, precision = 19, scale = 0)
    private BigDecimal totalNeto;

    @Column(nullable = false, length = 30)
    private String moneda;

    @Column(name = "monto_pagado", nullable = false, precision = 19, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "fondo_debitado", nullable = false, length = 30)
    private String fondoDebitado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
