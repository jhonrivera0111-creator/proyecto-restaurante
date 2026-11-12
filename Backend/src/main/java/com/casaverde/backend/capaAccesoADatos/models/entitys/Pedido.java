package com.casaverde.backend.capaAccesoADatos.models.entitys;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

/**
 * ENTIDAD JPA = una fila de la tabla "pedido" en SQLite.
 * Un pedido tiene muchos detalles (los productos) y muchos pagos.
 * Los totales (total, abonado, saldo) NO se guardan: se calculan al vuelo
 * (ver PedidoTotales) para que nunca queden desincronizados.
 *
 * Anotaciones Lombok: @Getter/@Setter generan los accesores, @Builder permite
 * Pedido.builder()...build(), @NoArgsConstructor lo exige JPA.
 */
@Entity
@Table(name = "pedido", indexes = {
        @Index(name = "idx_pedido_estado_creado", columnList = "estado, creado_en"),
        @Index(name = "idx_pedido_salon_id", columnList = "salon_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "integer")
    private Long id;

    /** Bloqueo optimista: si dos usuarios editan el mismo pedido a la vez, el segundo guardado falla en vez de pisar al primero. */
    @Version
    private Long version;

    @Column(length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salon_id")
    private Salon salon;

    @Column(length = 50)
    private String mesa;

    @Column(name = "fecha_reserva")
    private LocalDate fechaReserva;

    @Column(name = "hora_reserva")
    private LocalTime horaReserva;

    @Column(name = "cant_personas")
    private Integer cantPersonas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EstadoPedido estado;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "para_llevar")
    private Boolean paraLlevar;

    @Column(length = 30)
    private String telefono;

    /** Descuento en pesos aplicado a todo el pedido (no cambia el precio histórico de cada ítem). */
    @Column(name = "descuento_cop", nullable = false, precision = 19, scale = 0)
    @Builder.Default
    private BigDecimal descuentoCop = BigDecimal.ZERO;

    @Column(name = "descuento_aplicado_por", length = 150)
    private String descuentoAplicadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Column(name = "finalizado_en")
    private LocalDateTime finalizadoEn;

    @Column(name = "fiado_en")
    private LocalDateTime fiadoEn;

    // cascade ALL + orphanRemoval: guardar/borrar el pedido guarda/borra sus líneas y pagos automáticamente.
    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<PedidoDetalle> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private List<Pago> pagos = new ArrayList<>();

    /** Se ejecuta justo antes del INSERT: sella fechas en hora de Bogotá y estado inicial. */
    @PrePersist
    void onCreate() {
        creadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
        if (estado == null)
            estado = EstadoPedido.PENDIENTE;
    }

    /** Se ejecuta antes de cada UPDATE: refresca la fecha de última modificación. */
    @PreUpdate
    void onUpdate() {
        actualizadoEn = LocalDateTime.now(ZoneId.of("America/Bogota"));
    }
}
