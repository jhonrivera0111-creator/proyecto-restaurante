package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByEliminadoFalseOrderByOrdenAsc();

    List<Producto> findByActivoTrueAndEliminadoFalseOrderByOrdenAsc();

    Optional<Producto> findByIdAndEliminadoFalse(Long id);

    boolean existsByNombreAndEliminadoFalse(String nombre);

    boolean existsByNombreAndIdNotAndEliminadoFalse(String nombre, Long id);

    List<Producto> findByCategoriaId(Long categoriaId);
}
