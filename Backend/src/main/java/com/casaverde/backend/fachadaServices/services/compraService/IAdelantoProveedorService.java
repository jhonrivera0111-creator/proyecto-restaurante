package com.casaverde.backend.fachadaServices.services.compraService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.AdelantoProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.AdelantoProveedorRequest;

public interface IAdelantoProveedorService {
    AdelantoProveedorDTO registrar(AdelantoProveedorRequest req);
    List<AdelantoProveedorDTO> listarPorProveedor(Long proveedorId);
    List<AdelantoProveedorDTO> listarActivosPorProveedor(Long proveedorId);
}
