package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CierreCaja;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {
    Optional<CierreCaja> findByFecha(LocalDate fecha);

    List<CierreCaja> findAllByOrderByFechaDesc();

    boolean existsByFecha(LocalDate fecha);
}
