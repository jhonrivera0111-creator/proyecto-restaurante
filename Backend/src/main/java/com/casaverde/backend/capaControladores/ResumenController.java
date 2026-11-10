package com.casaverde.backend.capaControladores;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.ResumenDiaDTO;
import com.casaverde.backend.fachadaServices.services.pedidoService.IResumenService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/resumen")
@RequiredArgsConstructor
public class ResumenController {

    private final IResumenService service;

    @GetMapping
    public ResumenDiaDTO obtener(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.obtener(fecha != null ? fecha : LocalDate.now());
    }
}
