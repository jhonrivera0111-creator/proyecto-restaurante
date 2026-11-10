package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.ActualizarPrecioRequest;
import com.casaverde.backend.fachadaServices.dto.ProductoDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoPrecioHistorialDTO;
import com.casaverde.backend.fachadaServices.dto.ProductoRequest;
import com.casaverde.backend.fachadaServices.services.productoService.IProductoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final IProductoService service;

    @GetMapping
    public List<ProductoDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return soloActivos ? service.listarActivos() : service.listarTodos();
    }

    @GetMapping("/{id}")
    public ProductoDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @GetMapping("/{id}/precios")
    public List<ProductoPrecioHistorialDTO> historialPrecios(@PathVariable Long id) {
        return service.historialPrecios(id);
    }

    @PostMapping
    public ResponseEntity<ProductoDTO> crear(@Valid @RequestBody ProductoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @PutMapping("/reordenar")
    public List<ProductoDTO> reordenar(@RequestBody List<Long> idsEnOrden) {
        return service.reordenar(idsEnOrden);
    }

    @PutMapping("/{id}/precio")
    public ProductoDTO actualizarPrecio(@PathVariable Long id, @Valid @RequestBody ActualizarPrecioRequest req) {
        return service.actualizarPrecio(id, req.getPrecio(), req.getMotivo());
    }

    @PutMapping("/{id}/toggle")
    public ProductoDTO toggleActivo(@PathVariable Long id) {
        return service.toggleActivo(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
