package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.dto.HistorialAuditoriaDTO;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final IAuditoriaService service;

    @GetMapping
    public List<HistorialAuditoriaDTO> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId) {
        return service.listar(entidad, entidadId);
    }

    @GetMapping("/pagina")
    public Page<HistorialAuditoriaDTO> listarPaginado(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) Long entidadId,
            @PageableDefault(size = 25) Pageable pageable) {
        return service.listarPaginado(entidad, entidadId, pageable);
    }
}
