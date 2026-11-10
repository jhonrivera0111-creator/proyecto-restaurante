package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.PagoEmpleado;

public interface PagoEmpleadoRepository extends JpaRepository<PagoEmpleado, Long> {
    List<PagoEmpleado> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
}
