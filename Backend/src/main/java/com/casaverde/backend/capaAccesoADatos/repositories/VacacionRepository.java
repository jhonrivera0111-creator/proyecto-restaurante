package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Vacacion;

public interface VacacionRepository extends JpaRepository<Vacacion, Long> {
    List<Vacacion> findByEmpleadoIdOrderByCreadoEnDesc(Long empleadoId);
}
