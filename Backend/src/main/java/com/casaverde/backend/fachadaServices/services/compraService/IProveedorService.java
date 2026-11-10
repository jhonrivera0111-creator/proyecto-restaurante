package com.casaverde.backend.fachadaServices.services.compraService;

import java.util.List;
import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.ProveedorRequest;

public interface IProveedorService {
    List<ProveedorDTO> listar(boolean soloActivos);
    ProveedorDTO crear(ProveedorRequest req);
    ProveedorDTO actualizar(Long id, ProveedorRequest req);
    ProveedorDTO toggle(Long id);
}
