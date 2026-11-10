package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.casaverde.backend.fachadaServices.dto.HistorialAuditoriaDTO;

public interface IAuditoriaService {

    List<HistorialAuditoriaDTO> listar(String entidad, Long entidadId);

    Page<HistorialAuditoriaDTO> listarPaginado(String entidad, Long entidadId, Pageable pageable);

    void registrar(String entidad, Long entidadId, String accion, String detalle);

    /** Nombre del usuario autenticado actual (o "sistema-local" sin sesión). */
    String usuarioActual();
}
