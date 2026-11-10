package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gasto", indexes = {
        @Index(name = "idx_gasto_moneda_registrado", columnList = "moneda, registrado_en"),
        @Index(name = "idx_gasto_categoria_registrado", columnList = "categoria, registrado_en")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 300)
    private String concepto;

    @Column(nullable = false, length = 30)
    private String categoria; // insumos, servicios, equipos, otros

    @Column(nullable = false, length = 30)
    private String moneda; // COP, USD, NEQUI

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @Column(name = "monto_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal montoCop;

    @Column(name = "tasa_usada", precision = 19, scale = 2)
    private BigDecimal tasaUsada;

    @Column(name = "insumo_inventario", length = 100)
    private String insumoInventario;

    @Column(name = "cantidad_inventario", precision = 19, scale = 4)
    private BigDecimal cantidadInventario;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    void onCreate() {
        registradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
