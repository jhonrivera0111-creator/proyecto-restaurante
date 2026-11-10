package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 100)
    private String insumo;

    @Column(name = "stock_actual", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "alerta_minima", precision = 19, scale = 4)
    private BigDecimal alertaMinima;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String unidad = "unidad";

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    void onSave() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
