package com.casaverde.backend.fachadaServices.services.compraService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Proveedor;
import com.casaverde.backend.capaAccesoADatos.repositories.ProveedorRepository;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.ProveedorRequest;
import com.casaverde.backend.fachadaServices.mapper.ProveedorMapper;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProveedorServiceImpl implements IProveedorService {

    private final ProveedorRepository proveedorRepo;
    private final ProveedorMapper proveedorMapper;
    private final IAuditoriaService auditoriaService;

    @Transactional(readOnly = true)
    public List<ProveedorDTO> listar(boolean soloActivos) {
        List<Proveedor> lista = soloActivos
                ? proveedorRepo.findByActivoTrueOrderByNombreAsc()
                : proveedorRepo.findAllByOrderByNombreAsc();
        return lista.stream().map(proveedorMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ProveedorDTO crear(ProveedorRequest req) {
        Proveedor p = Proveedor.builder()
                .nombre(req.getNombre().trim())
                .tipo(req.getTipo())
                .telefono(req.getTelefono() != null ? req.getTelefono().trim() : null)
                .build();
        Proveedor guardado = proveedorRepo.save(p);
        auditoriaService.registrar("Proveedor", guardado.getId(), "CREAR", guardado.getNombre());
        return proveedorMapper.toDTO(guardado);
    }

    @Transactional
    public ProveedorDTO actualizar(Long id, ProveedorRequest req) {
        Proveedor p = proveedorRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
        p.setNombre(req.getNombre().trim());
        p.setTipo(req.getTipo());
        p.setTelefono(req.getTelefono() != null ? req.getTelefono().trim() : null);
        auditoriaService.registrar("Proveedor", p.getId(), "ACTUALIZAR", p.getNombre());
        return proveedorMapper.toDTO(p);
    }

    @Transactional
    public ProveedorDTO toggle(Long id) {
        Proveedor p = proveedorRepo.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
        p.setActivo(!p.getActivo());
        String accion = p.getActivo() ? "ACTIVAR" : "DESACTIVAR";
        auditoriaService.registrar("Proveedor", p.getId(), accion, p.getNombre());
        return proveedorMapper.toDTO(p);
    }
}
