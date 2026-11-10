package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDate;
import java.util.List;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Compra;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findAllByOrderByFechaDescCreadoEnDesc();
    List<Compra> findAllByOrderByCreadoEnDesc();
    List<Compra> findByProveedorIdOrderByFechaDescCreadoEnDesc(Long proveedorId);
    List<Compra> findByFechaOrderByCreadoEnDesc(LocalDate fecha);
    List<Compra> findByEstadoOrderByCreadoEnDesc(EstadoCompra estado);
    List<Compra> findByFechaAndEstadoOrderByCreadoEnDesc(LocalDate fecha, EstadoCompra estado);
}
