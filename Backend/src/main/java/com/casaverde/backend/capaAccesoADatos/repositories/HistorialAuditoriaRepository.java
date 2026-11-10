package com.casaverde.backend.capaAccesoADatos.repositories;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.casaverde.backend.capaAccesoADatos.models.entitys.HistorialAuditoria;

public interface HistorialAuditoriaRepository extends JpaRepository<HistorialAuditoria, Long> {
    List<HistorialAuditoria> findByEntidadAndEntidadIdOrderByRegistradoEnDesc(String entidad, Long entidadId);
    Page<HistorialAuditoria> findByEntidadAndEntidadIdOrderByRegistradoEnDesc(String entidad, Long entidadId, Pageable pageable);

    List<HistorialAuditoria> findByEntidadOrderByRegistradoEnDesc(String entidad);
    Page<HistorialAuditoria> findByEntidadOrderByRegistradoEnDesc(String entidad, Pageable pageable);

    List<HistorialAuditoria> findByEntidadIdOrderByRegistradoEnDesc(Long entidadId);
    Page<HistorialAuditoria> findByEntidadIdOrderByRegistradoEnDesc(Long entidadId, Pageable pageable);

    List<HistorialAuditoria> findAllByOrderByRegistradoEnDesc();
    Page<HistorialAuditoria> findAllByOrderByRegistradoEnDesc(Pageable pageable);
}
