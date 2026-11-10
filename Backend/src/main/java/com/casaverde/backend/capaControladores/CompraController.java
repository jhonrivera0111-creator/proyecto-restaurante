package com.casaverde.backend.capaControladores;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;
import com.casaverde.backend.fachadaServices.services.compraService.ICompraService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Tag(name = "Compras", description = "Compras de insumos, abonos y trazabilidad con proveedores")
public class CompraController {

    private final ICompraService compraService;

    @GetMapping
    @Operation(summary = "Listar compras", description = "Lista compras filtrables por fecha y estado")
    public List<CompraDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) EstadoCompra estado) {
        return compraService.listar(fecha, estado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener compra", description = "Consulta una compra con sus abonos y saldo")
    public CompraDTO obtener(@PathVariable Long id) {
        return compraService.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registrar compra", description = "Registra compra de insumo y aumenta inventario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compra registrada"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    public ResponseEntity<CompraDTO> registrar(@RequestBody @Valid CompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrar(req));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Anular compra", description = "Anula una compra pendiente y revierte inventario si hay stock suficiente")
    public ResponseEntity<Void> anular(@PathVariable Long id) {
        compraService.anular(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/abonos")
    public List<AbonoCompraDTO> listarAbonos(@PathVariable Long id) {
        return compraService.listarAbonos(id);
    }

    @PostMapping("/{id}/abonos")
    @Operation(summary = "Registrar abono", description = "Registra un abono normal o aplicado desde adelanto de proveedor")
    public ResponseEntity<AbonoCompraDTO> registrarAbono(
            @PathVariable Long id,
            @RequestBody @Valid AbonoCompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compraService.registrarAbono(id, req));
    }
}
