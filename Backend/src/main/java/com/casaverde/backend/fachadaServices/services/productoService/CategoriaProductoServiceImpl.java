package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CategoriaProducto;
import com.casaverde.backend.fachadaServices.dto.CategoriaProductoDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.CategoriaProductoMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.CategoriaProductoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoriaProductoServiceImpl implements ICategoriaProductoService {

    private final CategoriaProductoRepository repo;
    private final IAuditoriaService auditoriaService;
    private final CategoriaProductoMapper categoriaMapper;

    @Transactional(readOnly = true)
    public List<CategoriaProductoDTO> listarTodas() {
        return repo.findAll().stream().map(categoriaMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoriaProductoDTO> listarActivas() {
        return repo.findByActivoTrueOrderByOrdenAsc().stream().map(categoriaMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoriaProductoDTO buscarPorId(Long id) {
        return categoriaMapper.toDTO(findOrThrow(id));
    }

    @Transactional
    public CategoriaProductoDTO crear(CategoriaProductoDTO dto) {
        if (repo.existsByNombre(dto.getNombre())) {
            throw new ValidacionException("Ya existe una categoría con el nombre: " + dto.getNombre());
        }
        CategoriaProducto entity = CategoriaProducto.builder()
                .nombre(dto.getNombre())
                .orden(dto.getOrden() != null ? dto.getOrden() : 0)
                .activo(true)
                .build();
        CategoriaProducto guardado = repo.save(entity);
        auditoriaService.registrar("categoria_producto", guardado.getId(), "CREAR",
                "Categoría creada: " + guardado.getNombre());
        return categoriaMapper.toDTO(guardado);
    }

    @Transactional
    public CategoriaProductoDTO actualizar(Long id, CategoriaProductoDTO dto) {
        CategoriaProducto entity = findOrThrow(id);
        if (repo.existsByNombreAndIdNot(dto.getNombre(), id)) {
            throw new ValidacionException("Ya existe una categoría con el nombre: " + dto.getNombre());
        }
        entity.setNombre(dto.getNombre());
        if (dto.getOrden() != null)
            entity.setOrden(dto.getOrden());
        if (dto.getActivo() != null)
            entity.setActivo(dto.getActivo());
        CategoriaProducto guardado = repo.save(entity);
        auditoriaService.registrar("categoria_producto", guardado.getId(), "ACTUALIZAR",
                "Categoría actualizada: " + guardado.getNombre());
        return categoriaMapper.toDTO(guardado);
    }

    @Transactional
    public void toggleActivo(Long id) {
        CategoriaProducto entity = findOrThrow(id);
        entity.setActivo(!entity.getActivo());
        CategoriaProducto guardado = repo.save(entity);
        auditoriaService.registrar("categoria_producto", guardado.getId(), "CAMBIAR_ESTADO",
                "Categoría activo=" + guardado.getActivo());
    }

    private CategoriaProducto findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría no encontrada: " + id));
    }

}
