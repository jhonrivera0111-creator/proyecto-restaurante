package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.ComparativoMensualDTO;
import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;
import com.casaverde.backend.fachadaServices.services.inventarioService.IInventarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
@Tag(name = "Inventario", description = "Stock actual, movimientos, alertas y comparativos de insumos")
public class InventarioController {

    private final IInventarioService service;

    @GetMapping
    @Operation(summary = "Listar stock", description = "Consulta el stock actual de todos los insumos")
    public List<InventarioDTO> listarStock() {
        return service.listarStock();
    }

    @GetMapping("/{insumo}")
    @Operation(summary = "Obtener stock", description = "Consulta stock y alerta de un insumo")
    public InventarioDTO obtenerStock(@PathVariable String insumo) {
        return service.obtenerStock(insumo);
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Listar movimientos", description = "Consulta el historial de inventario, opcionalmente por insumo")
    public List<MovimientoInventarioDTO> listarMovimientos(
            @RequestParam(required = false) String insumo) {
        return insumo != null
                ? service.listarMovimientosPorInsumo(insumo)
                : service.listarMovimientos();
    }

    @GetMapping("/movimientos/pagina")
    @Operation(summary = "Listar movimientos paginados", description = "Consulta el historial de inventario con paginacion")
    public Page<MovimientoInventarioDTO> listarMovimientosPaginados(
            @RequestParam(required = false) String insumo,
            @PageableDefault(size = 25) Pageable pageable) {
        return service.listarMovimientosPaginado(insumo, pageable);
    }

    @PostMapping("/movimiento")
    @Operation(summary = "Registrar movimiento manual", description = "Registra entrada o ajuste manual de inventario")
    public ResponseEntity<MovimientoInventarioDTO> registrarMovimiento(@Valid @RequestBody MovimientoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarMovimiento(req));
    }

    @PatchMapping("/{insumo}/alerta")
    public InventarioDTO actualizarAlerta(
            @PathVariable String insumo,
            @RequestBody Map<String, BigDecimal> body) {
        return service.actualizarAlerta(insumo, body.get("alertaMinima"));
    }

    @GetMapping("/comparativo")
    public ComparativoMensualDTO comparativoAnual(
            @RequestParam(required = false) Integer año) {
        int anio = año != null ? año : LocalDate.now().getYear();
        return service.comparativoAnual(anio);
    }
}
