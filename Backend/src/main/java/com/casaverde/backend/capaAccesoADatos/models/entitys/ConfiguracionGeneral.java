package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuracion_general")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionGeneral {

    @Id
    @Column(length = 100)
    private String clave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valor;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @PrePersist
    @PreUpdate
    void onSave() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
