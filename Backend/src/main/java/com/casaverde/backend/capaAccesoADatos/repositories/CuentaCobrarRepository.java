package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CuentaCobrar;
import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCuentaCobrar;

public interface CuentaCobrarRepository extends JpaRepository<CuentaCobrar, Long> {
    List<CuentaCobrar> findAllByOrderByCreadoEnDesc();
    List<CuentaCobrar> findByEstadoOrderByCreadoEnDesc(EstadoCuentaCobrar estado);
    List<CuentaCobrar> findByEstadoAndFechaOrderByCreadoEnDesc(EstadoCuentaCobrar estado, LocalDate fecha);
    List<CuentaCobrar> findByFechaOrderByCreadoEnDesc(LocalDate fecha);
    List<CuentaCobrar> findByClienteNombreContainingIgnoreCaseOrderByCreadoEnDesc(String q);
}
