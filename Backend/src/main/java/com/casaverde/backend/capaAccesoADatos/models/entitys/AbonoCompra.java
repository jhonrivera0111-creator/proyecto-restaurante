package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "abono_compra", indexes = {
        @Index(name = "idx_abono_compra_compra", columnList = "compra_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonoCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(nullable = false, length = 30)
    private String moneda;

    @Column(name = "monto_cop", precision = 19, scale = 0, nullable = false)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "fondo_debitado", nullable = false, length = 30)
    private String fondoDebitado;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "adelanto_proveedor_id")
    private AdelantoProveedor adelantoProveedor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
