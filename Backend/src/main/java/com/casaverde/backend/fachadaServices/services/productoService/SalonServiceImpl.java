package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Salon;
import com.casaverde.backend.fachadaServices.dto.SalonDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.SalonMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.SalonRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SalonServiceImpl implements ISalonService {

    private final SalonRepository repo;
    private final IAuditoriaService auditoriaService;
    private final SalonMapper salonMapper;

    @Transactional(readOnly = true)
    public List<SalonDTO> listarTodos() {
        return repo.findByEliminadoFalseOrderByNombreAsc().stream()
                .map(salonMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalonDTO> listarActivos() {
        return repo.findByActivoTrueAndEliminadoFalseOrderByNombreAsc().stream()
                .map(salonMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalonDTO buscarPorId(Long id) {
        return salonMapper.toDTO(findOrThrow(id));
    }

    @Transactional
    public SalonDTO crear(SalonDTO dto) {
        String nombre = dto.getNombre() != null ? dto.getNombre().trim() : "";
        if (nombre.isEmpty()) {
            throw new ValidacionException("El nombre del salón es obligatorio");
        }
        Optional<Salon> eliminado = repo.findByNombreAndEliminadoTrue(nombre);
        if (eliminado.isPresent()) {
            Salon salon = eliminado.get();
            salon.setEliminado(false);
            salon.setActivo(true);
            salon.setAforo(dto.getAforo());
            Salon guardado = repo.save(salon);
            auditoriaService.registrar("salon", guardado.getId(), "REACTIVAR",
                    "Salón reactivado: " + guardado.getNombre());
            return salonMapper.toDTO(guardado);
        }
        validarNombre(nombre, null);
        Salon salon = Salon.builder()
                .nombre(nombre)
                .aforo(dto.getAforo())
                .activo(true)
                .build();
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "CREAR",
                "Salón creado: " + guardado.getNombre());
        return salonMapper.toDTO(guardado);
    }

    @Transactional
    public SalonDTO actualizar(Long id, SalonDTO dto) {
        Salon salon = findOrThrow(id);
        validarNombre(dto.getNombre(), id);
        salon.setNombre(dto.getNombre().trim());
        salon.setAforo(dto.getAforo());
        if (dto.getActivo() != null)
            salon.setActivo(dto.getActivo());
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "ACTUALIZAR",
                "Salón actualizado: " + guardado.getNombre());
        return salonMapper.toDTO(guardado);
    }

    @Transactional
    public SalonDTO toggleActivo(Long id) {
        Salon salon = findOrThrow(id);
        salon.setActivo(!salon.getActivo());
        Salon guardado = repo.save(salon);
        auditoriaService.registrar("salon", guardado.getId(), "CAMBIAR_ESTADO",
                "Salón activo=" + guardado.getActivo());
        return salonMapper.toDTO(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        Salon salon = findOrThrow(id);
        salon.setEliminado(true);
        salon.setActivo(false);
        repo.save(salon);
        auditoriaService.registrar("salon", salon.getId(), "ELIMINAR",
                "Salón eliminado: " + salon.getNombre());
    }

    private void validarNombre(String nombre, Long idExcluir) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ValidacionException("El nombre del salón es obligatorio");
        }
        boolean existe = idExcluir == null
                ? repo.existsByNombreAndEliminadoFalse(nombre.trim())
                : repo.existsByNombreAndIdNotAndEliminadoFalse(nombre.trim(), idExcluir);
        if (existe) {
            throw new ValidacionException("Ya existe un salón con el nombre: " + nombre.trim());
        }
    }

    private Salon findOrThrow(Long id) {
        return repo.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Salón no encontrado: " + id));
    }
}
