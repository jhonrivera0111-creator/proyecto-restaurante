package com.casaverde.backend.capaAccesoADatos.repositories;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Gasto;

public interface GastoRepository extends JpaRepository<Gasto, Long> {
    List<Gasto> findByRegistradoEnBetweenOrderByRegistradoEnDesc(LocalDateTime inicio, LocalDateTime fin);

    List<Gasto> findAllByOrderByRegistradoEnDesc();
}
