package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Salon;

public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findByEliminadoFalseOrderByNombreAsc();

    List<Salon> findByActivoTrueAndEliminadoFalseOrderByNombreAsc();

    boolean existsByNombreAndEliminadoFalse(String nombre);

    boolean existsByNombreAndIdNotAndEliminadoFalse(String nombre, Long id);

    Optional<Salon> findByNombreAndEliminadoTrue(String nombre);

    Optional<Salon> findByIdAndEliminadoFalse(Long id);
}
