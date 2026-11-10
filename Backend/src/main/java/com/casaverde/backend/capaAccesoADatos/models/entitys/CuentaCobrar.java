package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCuentaCobrar;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cuenta_cobrar", indexes = {
        @Index(name = "idx_cc_estado_creado", columnList = "estado, creado_en"),
        @Index(name = "idx_cc_cliente_nombre", columnList = "cliente_nombre")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    private String concepto;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EstadoCuentaCobrar estado = EstadoCuentaCobrar.PENDIENTE;

    @Column(length = 20)
    private String celular;

    @Column(nullable = false)
    private LocalDate fecha;

    private String observacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (estado == null) estado = EstadoCuentaCobrar.PENDIENTE;
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
