package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @EntityGraph(attributePaths = { "salon", "detalles" })
    List<Pedido> findByEstadoOrderByCreadoEnDesc(EstadoPedido estado);

    @EntityGraph(attributePaths = { "salon", "detalles" })
    List<Pedido> findByEstadoInOrderByCreadoEnDesc(List<EstadoPedido> estados);

    @EntityGraph(attributePaths = { "salon", "detalles" })
    List<Pedido> findAllByOrderByCreadoEnDesc();

    @EntityGraph(attributePaths = { "salon" })
    Page<Pedido> findAllByOrderByCreadoEnDesc(Pageable pageable);

    List<Pedido> findByCreadoEnBetweenOrderByCreadoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Pedido> findByFinalizadoEnBetweenOrderByFinalizadoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Pedido> findByFiadoEnBetweenOrderByFiadoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    long countByEstadoIn(List<EstadoPedido> estados);

    /**
     * Pedidos activos "relevantes" a una fecha de cierre.
     * - Pedido sin reserva (fechaReserva IS NULL): cuenta siempre que esté activo,
     *   sin restricción de fecha — mismo criterio que existía antes de introducir
     *   el manejo de reservas (countByEstadoIn).
     * - Pedido con reserva: cuenta únicamente si fechaReserva es EXACTAMENTE la
     *   fecha del cierre. Una reserva de un día anterior o posterior no es "de
     *   este día" y no debe bloquear el cierre de esta fecha.
     */
    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.estado IN :estados "
            + "AND (p.fechaReserva IS NULL OR p.fechaReserva = :fecha)")
    long countActivosParaFecha(@Param("estados") List<EstadoPedido> estados, @Param("fecha") LocalDate fecha);
}
