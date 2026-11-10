package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caja_movimiento", indexes = {
        @Index(name = "idx_cm_moneda_registrado", columnList = "moneda, registrado_en"),
        @Index(name = "idx_cm_referencia", columnList = "referencia_tipo, referencia_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CajaMovimientoTipo tipo;

    @Column(nullable = false, length = 30)
    private String moneda;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "saldo_resultante", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoResultante;

    @Column(name = "referencia_tipo", length = 50)
    private String referenciaTipo;

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(nullable = false, length = 300)
    private String concepto;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    void onCreate() {
        registradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
