package com.casaverde.backend.capaAccesoADatos.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;

public interface FondoRepository extends JpaRepository<Fondo, String> {
}
