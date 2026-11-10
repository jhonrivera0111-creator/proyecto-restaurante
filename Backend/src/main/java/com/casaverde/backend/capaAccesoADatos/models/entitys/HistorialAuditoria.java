package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "historial_auditoria", indexes = {
        @Index(name = "idx_auditoria_entidad", columnList = "entidad, entidad_id"),
        @Index(name = "idx_auditoria_registrado", columnList = "registrado_en")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false, length = 100)
    private String entidad;

    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @Column(nullable = false, length = 150)
    @Builder.Default
    private String usuario = "sistema-local";

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    void onCreate() {
        if (usuario == null || usuario.isBlank()) {
            usuario = "sistema-local";
        }
        registradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
