package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findAllByOrderByNombreAsc();
    List<Proveedor> findByActivoTrueOrderByNombreAsc();
}
