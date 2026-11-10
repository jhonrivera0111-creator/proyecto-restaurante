package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.casaverde.backend.capaAccesoADatos.models.entitys.PedidoDetalle;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoPedido;

public interface PedidoDetalleRepository extends JpaRepository<PedidoDetalle, Long> {

        /**
         * Detalles de pedidos activos que referencian el producto dado.
         * Se usa para propagar un cambio real de precio.
         */
        @Query("SELECT d FROM PedidoDetalle d WHERE d.producto.id = :productoId AND d.pedido.estado IN :estados")
        List<PedidoDetalle> findByProductoIdAndPedidoEstadoIn(
                        @Param("productoId") Long productoId,
                        @Param("estados") List<EstadoPedido> estados);

        /**
         * Detalles de pedidos activos cuyo precioUnitarioCop difiere del precio
         * actual del producto referenciado. Se usa para el backfill de arranque:
         * detecta desfases sin alterar el historial de precios del producto.
         */
        @Query("""
                        SELECT d FROM PedidoDetalle d
                        JOIN d.producto p
                        WHERE d.producto IS NOT NULL
                          AND d.pedido.estado IN :estados
                          AND d.precioUnitarioCop <> p.precio
                        """)
        List<PedidoDetalle> findDesincronizados(@Param("estados") List<EstadoPedido> estados);
}
