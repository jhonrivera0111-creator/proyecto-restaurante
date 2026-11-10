package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.CategoriaProductoDTO;

public interface ICategoriaProductoService {

    List<CategoriaProductoDTO> listarTodas();

    List<CategoriaProductoDTO> listarActivas();

    CategoriaProductoDTO buscarPorId(Long id);

    CategoriaProductoDTO crear(CategoriaProductoDTO dto);

    CategoriaProductoDTO actualizar(Long id, CategoriaProductoDTO dto);

    void toggleActivo(Long id);
}
