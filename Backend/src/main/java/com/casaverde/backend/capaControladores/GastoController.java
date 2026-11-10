package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.GastoDTO;
import com.casaverde.backend.fachadaServices.dto.GastoRequest;
import com.casaverde.backend.fachadaServices.services.cajaService.IGastoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/gastos")
@RequiredArgsConstructor
public class GastoController {

    private final IGastoService service;

    @GetMapping
    public List<GastoDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return fecha != null ? service.listarPorFecha(fecha) : service.listarTodos();
    }

    @PostMapping
    public ResponseEntity<GastoDTO> registrar(@Valid @RequestBody GastoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
