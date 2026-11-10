package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedido_detalle", indexes = {
        @Index(name = "idx_pd_pedido_id", columnList = "pedido_id"),
        @Index(name = "idx_pd_producto_id", columnList = "producto_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(name = "nombre_snapshot", nullable = false)
    private String nombreSnapshot;

    @Column(name = "precio_unitario_cop", nullable = false, precision = 19, scale = 0)
    private BigDecimal precioUnitarioCop;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "composicion_snapshot", columnDefinition = "TEXT")
    private String composicionSnapshot;
}
