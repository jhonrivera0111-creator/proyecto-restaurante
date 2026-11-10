package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AdelantoProveedor;

public interface AdelantoProveedorRepository extends JpaRepository<AdelantoProveedor, Long> {
    List<AdelantoProveedor> findByProveedorIdOrderByFechaDesc(Long proveedorId);
    List<AdelantoProveedor> findByProveedorIdAndActivoTrueOrderByFechaAsc(Long proveedorId);
}
