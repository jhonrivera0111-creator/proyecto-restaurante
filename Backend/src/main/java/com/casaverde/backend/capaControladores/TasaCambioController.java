package com.casaverde.backend.capaControladores;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.TasaCambioDTO;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tasa-cambio")
@RequiredArgsConstructor
public class TasaCambioController {

    private final ITasaCambioService service;

    @GetMapping("/vigente")
    public TasaCambioDTO obtenerVigente() {
        return service.obtenerVigente();
    }

    @GetMapping("/historial")
    public List<TasaCambioDTO> historial() {
        return service.historial();
    }

    @PostMapping
    public ResponseEntity<TasaCambioDTO> registrar(@RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(body.get("valorCop")));
    }
}
