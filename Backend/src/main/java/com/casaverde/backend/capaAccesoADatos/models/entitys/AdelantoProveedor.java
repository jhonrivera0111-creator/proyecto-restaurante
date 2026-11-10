package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "adelanto_proveedor", indexes = {
        @Index(name = "idx_adelanto_prov_proveedor_activo", columnList = "proveedor_id, activo")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdelantoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Column(name = "proveedor_nombre", nullable = false, length = 150)
    private String proveedorNombreSnapshot;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal monto;

    @Column(name = "saldo_restante", nullable = false, precision = 19, scale = 0)
    private BigDecimal saldoRestante;

    @Column(nullable = false, length = 30)
    private String moneda;

    @Column(name = "monto_cop_inicial", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCopInicial;

    @Column(name = "fondo_debitado", nullable = false, length = 30)
    private String fondoDebitado;

    @Column(nullable = false, length = 100)
    private String insumo;

    @Column(name = "precio_unitario_acordado", nullable = false, precision = 19, scale = 0)
    private BigDecimal precioUnitarioAcordado;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (activo == null) activo = true;
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
