package com.casaverde.backend.capaAccesoADatos.models.entitys;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "producto_precio_historial", indexes = @Index(name = "idx_precio_producto", columnList = "producto_id, registrado_en"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoPrecioHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "producto_nombre", nullable = false, length = 150)
    private String productoNombre;

    @Column(name = "precio_anterior", precision = 19, scale = 0)
    private BigDecimal precioAnterior;

    @Column(name = "precio_nuevo", nullable = false, precision = 19, scale = 0)
    private BigDecimal precioNuevo;

    @Column(nullable = false, length = 200)
    private String motivo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @PrePersist
    void onCreate() {
        registradoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
