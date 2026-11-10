package com.casaverde.backend.fachadaServices.services.productoService;

import java.math.BigDecimal;
import java.util.List;

import com.casaverde.backend.fachadaServices.dto.ProductoDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoPrecioHistorialDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoRequest;

public interface IProductoService {

    List<ProductoDTO> listarTodos();

    List<ProductoDTO> listarActivos();

    ProductoDTO buscarPorId(Long id);

    List<ProductoPrecioHistorialDTO> historialPrecios(Long id);

    ProductoDTO crear(ProductoRequest req);

    ProductoDTO actualizar(Long id, ProductoRequest req);

    ProductoDTO actualizarPrecio(Long id, BigDecimal precio, String motivo);

    ProductoDTO toggleActivo(Long id);

    void eliminar(Long id);

    /** Reordena el catálogo: idsEnOrden es la lista completa de ids en el orden deseado. */
    List<ProductoDTO> reordenar(List<Long> idsEnOrden);
}
