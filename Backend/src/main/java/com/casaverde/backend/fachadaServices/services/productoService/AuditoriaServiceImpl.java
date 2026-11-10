package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.casaverde.backend.config.security.AuthTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.HistorialAuditoria;
import com.casaverde.backend.fachadaServices.dto.HistorialAuditoriaDTO;
import com.casaverde.backend.fachadaServices.mapper.AuditoriaMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.HistorialAuditoriaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements IAuditoriaService {

    private final HistorialAuditoriaRepository repo;
    private final AuditoriaMapper auditoriaMapper;

    @Transactional(readOnly = true)
    public List<HistorialAuditoriaDTO> listar(String entidad, Long entidadId) {
        if (entidad != null && !entidad.isBlank() && entidadId != null) {
            return repo.findByEntidadAndEntidadIdOrderByRegistradoEnDesc(entidad.trim(), entidadId)
                    .stream().map(auditoriaMapper::toDTO).collect(Collectors.toList());
        }
        if (entidad != null && !entidad.isBlank()) {
            return repo.findByEntidadOrderByRegistradoEnDesc(entidad.trim())
                    .stream().map(auditoriaMapper::toDTO).collect(Collectors.toList());
        }
        if (entidadId != null) {
            return repo.findByEntidadIdOrderByRegistradoEnDesc(entidadId)
                    .stream().map(auditoriaMapper::toDTO).collect(Collectors.toList());
        }
        return repo.findAllByOrderByRegistradoEnDesc()
                .stream().map(auditoriaMapper::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistorialAuditoriaDTO> listarPaginado(String entidad, Long entidadId, Pageable pageable) {
        if (entidad != null && !entidad.isBlank() && entidadId != null) {
            return repo.findByEntidadAndEntidadIdOrderByRegistradoEnDesc(entidad.trim(), entidadId, pageable)
                    .map(auditoriaMapper::toDTO);
        }
        if (entidad != null && !entidad.isBlank()) {
            return repo.findByEntidadOrderByRegistradoEnDesc(entidad.trim(), pageable)
                    .map(auditoriaMapper::toDTO);
        }
        if (entidadId != null) {
            return repo.findByEntidadIdOrderByRegistradoEnDesc(entidadId, pageable)
                    .map(auditoriaMapper::toDTO);
        }
        return repo.findAllByOrderByRegistradoEnDesc(pageable)
                .map(auditoriaMapper::toDTO);
    }

    @Transactional
    public void registrar(String entidad, Long entidadId, String accion, String detalle) {
        repo.save(HistorialAuditoria.builder()
                .entidad(entidad)
                .entidadId(entidadId)
                .accion(accion)
                .detalle(detalle)
                .usuario(resolverUsuario())
                .build());
    }

    @Override
    public String usuarioActual() {
        return resolverUsuario();
    }

    private String resolverUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof AuthTokenService.UsuarioAutenticado u) {
                return u.nombre();
            }
            if (!"anonymousUser".equals(principal)) {
                return auth.getName();
            }
        }
        return "sistema-local";
    }

}
