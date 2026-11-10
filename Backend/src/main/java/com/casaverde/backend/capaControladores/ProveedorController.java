package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.casaverde.backend.fachadaServices.dto.AdelantoProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.AdelantoProveedorRequest;
import com.casaverde.backend.fachadaServices.dto.ProveedorDTO;
import com.casaverde.backend.fachadaServices.dto.ProveedorRequest;
import com.casaverde.backend.fachadaServices.services.compraService.IAdelantoProveedorService;
import com.casaverde.backend.fachadaServices.services.compraService.IProveedorService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final IProveedorService proveedorService;
    private final IAdelantoProveedorService adelantoProveedorService;

    @GetMapping
    public List<ProveedorDTO> listar(@RequestParam(defaultValue = "false") boolean soloActivos) {
        return proveedorService.listar(soloActivos);
    }

    @PostMapping
    public ResponseEntity<ProveedorDTO> crear(@RequestBody @Valid ProveedorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.crear(req));
    }

    @PutMapping("/{id}")
    public ProveedorDTO actualizar(@PathVariable Long id, @RequestBody @Valid ProveedorRequest req) {
        return proveedorService.actualizar(id, req);
    }

    @PutMapping("/{id}/toggle")
    public ProveedorDTO toggle(@PathVariable Long id) {
        return proveedorService.toggle(id);
    }

    @GetMapping("/{id}/adelantos")
    public List<AdelantoProveedorDTO> listarAdelantos(@PathVariable Long id) {
        return adelantoProveedorService.listarPorProveedor(id);
    }

    @PostMapping("/{id}/adelantos")
    public ResponseEntity<AdelantoProveedorDTO> registrarAdelanto(
            @PathVariable Long id, @RequestBody AdelantoProveedorRequest req) {
        req.setProveedorId(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(adelantoProveedorService.registrar(req));
    }
}
