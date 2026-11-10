package com.casaverde.backend.capaControladores;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.services.productoService.IExportacionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/exportaciones")
@RequiredArgsConstructor
public class ExportacionController {

    private static final MediaType TEXT_CSV_UTF_8 = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final IExportacionService service;

    @GetMapping("/resumen")
    public ResponseEntity<String> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate fechaFinal = fecha != null ? fecha : LocalDate.now();
        return csv("resumen-" + fechaFinal + ".csv", service.resumenCsv(fechaFinal));
    }

    @GetMapping("/caja")
    public ResponseEntity<String> caja(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        LocalDate fechaFinal = fecha != null ? fecha : LocalDate.now();
        return csv("caja-" + fechaFinal + ".csv", service.cajaCsv(fechaFinal));
    }

    private ResponseEntity<String> csv(String filename, String body) {
        return ResponseEntity.ok()
                .contentType(TEXT_CSV_UTF_8)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(body);
    }
}
