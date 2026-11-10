package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;
import com.casaverde.backend.fachadaServices.dto.SalonDTO;

public interface ISalonService {
    List<SalonDTO> listarTodos();
    List<SalonDTO> listarActivos();
    SalonDTO buscarPorId(Long id);
    SalonDTO crear(SalonDTO dto);
    SalonDTO actualizar(Long id, SalonDTO dto);
    SalonDTO toggleActivo(Long id);
    void eliminar(Long id);
}
