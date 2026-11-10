package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoComposicionInventario;

public interface ProductoComposicionRepository extends JpaRepository<ProductoComposicionInventario, Long> {
    List<ProductoComposicionInventario> findByProductoId(Long productoId);

    void deleteByProductoId(Long productoId);
}
