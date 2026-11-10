package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.ProductoPrecioHistorial;

public interface ProductoPrecioHistorialRepository extends JpaRepository<ProductoPrecioHistorial, Long> {
    List<ProductoPrecioHistorial> findByProducto_IdOrderByRegistradoEnDesc(Long productoId);

    List<ProductoPrecioHistorial> findAllByOrderByRegistradoEnDesc();
}
