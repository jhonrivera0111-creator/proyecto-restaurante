package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.CategoriaProductoDTO;
import com.casaverde.backend.fachadaServices.services.productoService.ICategoriaProductoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaProductoController {

    private final ICategoriaProductoService service;

    @GetMapping
    public List<CategoriaProductoDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivas) {
        return soloActivas ? service.listarActivas() : service.listarTodas();
    }

    @GetMapping("/{id}")
    public CategoriaProductoDTO buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaProductoDTO> crear(@RequestBody CategoriaProductoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/{id}")
    public CategoriaProductoDTO actualizar(@PathVariable Long id, @RequestBody CategoriaProductoDTO dto) {
        return service.actualizar(id, dto);
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        service.toggleActivo(id);
        return ResponseEntity.ok().build();
    }
}
