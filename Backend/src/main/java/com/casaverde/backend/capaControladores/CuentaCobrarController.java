package com.casaverde.backend.capaControladores;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCuentaCobrar;
import com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarRequest;
import com.casaverde.backend.fachadaServices.dto.CuentaCobrarDTO;
import com.casaverde.backend.fachadaServices.dto.CuentaCobrarRequest;
import com.casaverde.backend.fachadaServices.services.pedidoService.ICuentaCobrarService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/cuentas-cobrar")
@RequiredArgsConstructor
public class CuentaCobrarController {

    private final ICuentaCobrarService cuentaCobrarService;

    @GetMapping
    public List<CuentaCobrarDTO> listar(
            @RequestParam(required = false) EstadoCuentaCobrar estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String q) {
        return cuentaCobrarService.listar(estado, fecha, q);
    }

    @GetMapping("/{id}")
    public CuentaCobrarDTO obtener(@PathVariable Long id) {
        return cuentaCobrarService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<CuentaCobrarDTO> registrar(@RequestBody @Valid CuentaCobrarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaCobrarService.registrar(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        cuentaCobrarService.anular(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/abonos")
    public List<AbonoCuentaCobrarDTO> listarAbonos(@PathVariable Long id) {
        return cuentaCobrarService.listarAbonos(id);
    }

    @PostMapping("/{id}/abonos")
    public ResponseEntity<AbonoCuentaCobrarDTO> registrarAbono(
            @PathVariable Long id, @RequestBody @Valid AbonoCuentaCobrarRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaCobrarService.registrarAbono(id, req));
    }
}
