package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Adelanto;

public interface AdelantoRepository extends JpaRepository<Adelanto, Long> {
    List<Adelanto> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
    List<Adelanto> findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(Long empleadoId);
}
