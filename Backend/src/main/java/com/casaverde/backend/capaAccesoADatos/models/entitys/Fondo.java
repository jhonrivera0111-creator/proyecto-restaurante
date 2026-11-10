package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fondo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fondo {

    @Id
    @Column(length = 30)
    private String moneda; // COP, USD, NEQUI, NEQUI_2, NEQUI_HAROLD, etc.

    @Version
    private Long version;

    @Column(length = 100)
    private String alias;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    void onCreate() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
