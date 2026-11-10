package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tasa_cambio", indexes = {
        @Index(name = "idx_tasa_vigente_desde", columnList = "vigente_desde")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TasaCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(name = "valor_cop", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorCop;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDateTime vigenteDesde;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @PrePersist
    void onCreate() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
        if (vigenteDesde == null) {
            vigenteDesde = LocalDateTime.now(ZoneId.of("America/Bogota"));
        }
    }
}
