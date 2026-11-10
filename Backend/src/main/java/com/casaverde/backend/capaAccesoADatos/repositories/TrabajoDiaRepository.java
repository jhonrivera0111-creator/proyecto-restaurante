package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.TrabajoDia;

public interface TrabajoDiaRepository extends JpaRepository<TrabajoDia, Long> {
    List<TrabajoDia> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
    List<TrabajoDia> findByEmpleadoIdAndPagadoFalseOrderByFechaAsc(Long empleadoId);
}
