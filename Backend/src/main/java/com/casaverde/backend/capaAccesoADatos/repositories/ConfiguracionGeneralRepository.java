package com.casaverde.backend.capaAccesoADatos.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.ConfiguracionGeneral;

public interface ConfiguracionGeneralRepository extends JpaRepository<ConfiguracionGeneral, String> {
}
