package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;
import com.casaverde.backend.fachadaServices.dto.CierreCajaRequest;
import com.casaverde.backend.fachadaServices.services.pedidoService.ICierreCajaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cierres-caja")
@RequiredArgsConstructor
public class CierreCajaController {

    private final ICierreCajaService service;

    @GetMapping
    public List<CierreCajaDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{fecha}")
    public CierreCajaDTO obtener(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.obtener(fecha);
    }

    @PostMapping
    public CierreCajaDTO cerrar(@Valid @RequestBody CierreCajaRequest request) {
        return service.cerrar(request);
    }
}
