package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoAdelanto;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "adelanto", indexes = {
        @Index(name = "idx_adelanto_empleado_pagado", columnList = "empleado_id, pagado")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adelanto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'EFECTIVO'")
    @Builder.Default
    private TipoAdelanto tipo = TipoAdelanto.EFECTIVO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 30)
    private String moneda;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "fondo_debitado", nullable = false, length = 30)
    private String fondoDebitado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pagado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (tipo == null) tipo = TipoAdelanto.EFECTIVO;
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
