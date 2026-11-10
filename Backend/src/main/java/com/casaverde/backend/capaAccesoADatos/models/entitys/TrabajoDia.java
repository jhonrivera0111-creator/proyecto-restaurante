package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trabajo_dia",
        uniqueConstraints = @UniqueConstraint(name = "uk_trabajo_dia_empleado_fecha", columnNames = {"empleado_id", "fecha"}),
        indexes = @Index(name = "idx_trabajo_dia_empleado_pagado", columnList = "empleado_id, pagado"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrabajoDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(nullable = false)
    private LocalDate fecha;

    private String observacion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean pagado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
