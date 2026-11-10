package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    Optional<Inventario> findByInsumo(String insumo);

    List<Inventario> findAllByOrderByInsumoAsc();

    boolean existsByInsumo(String insumo);
}
