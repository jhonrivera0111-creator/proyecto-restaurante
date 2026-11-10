package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "producto", indexes = {
        @Index(name = "idx_producto_categoria_id", columnList = "categoria_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, precision = 19, scale = 0)
    @Builder.Default
    private BigDecimal precio = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private CategoriaProducto categoria;

    @Column(name = "afecta_inventario", nullable = false)
    @Builder.Default
    private Boolean afectaInventario = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean eliminado = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Column(name = "creado_en")
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<ProductoComposicionInventario> composicion = new ArrayList<>();

    @PrePersist
    void onCreate() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }

    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
