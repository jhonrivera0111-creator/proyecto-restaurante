package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "movimiento_inventario", indexes = {
        @Index(name = "idx_mi_tipo_ref_insumo", columnList = "tipo, referencia_tipo, insumo"),
        @Index(name = "idx_mi_insumo_registrado", columnList = "insumo, registrado_en")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Column(nullable = false, length = 100)
    private String insumo;

    @Column(nullable = false, length = 20)
    private String tipo; // entrada | salida | ajuste

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "stock_resultante", nullable = false, precision = 19, scale = 4)
    private BigDecimal stockResultante;

    @Column(name = "referencia_tipo", length = 30)
    private String referenciaTipo; // pedido | gasto | ajuste_manual

    @Column(name = "referencia_id")
    private Long referenciaId;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    void onCreate() {
        registradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
