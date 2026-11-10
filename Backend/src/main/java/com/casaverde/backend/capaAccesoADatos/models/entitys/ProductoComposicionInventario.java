package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "producto_composicion_inventario", uniqueConstraints = @UniqueConstraint(columnNames = { "producto_id",
        "insumo" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoComposicionInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 100)
    private String insumo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cantidad;
}
