package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "abono_cuenta_cobrar", indexes = {
        @Index(name = "idx_abono_cc_cuenta", columnList = "cuenta_cobrar_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCuentaCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_cobrar_id", nullable = false)
    private CuentaCobrar cuenta;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 30)
    private String moneda;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "fondo_acreditado", nullable = false, length = 30)
    private String fondoAcreditado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
