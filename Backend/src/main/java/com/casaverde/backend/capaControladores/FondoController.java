package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.AjusteCapitalRequest;
import com.casaverde.backend.fachadaServices.dto.EntradaFondoRequest;
import com.casaverde.backend.fachadaServices.dto.EstadoNequiMesDTO;
import com.casaverde.backend.fachadaServices.dto.FondoDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoFondoDTO;
import com.casaverde.backend.fachadaServices.dto.TransferenciaFondoRequest;
import com.casaverde.backend.fachadaServices.services.cajaService.IFondoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/fondos")
@RequiredArgsConstructor
@Tag(name = "Fondos", description = "Saldos, movimientos de caja, entradas, transferencias y cuentas Nequi")
public class FondoController {

    private final IFondoService service;

    @GetMapping
    @Operation(summary = "Listar fondos", description = "Consulta saldos actuales de COP, USD, NEQUI y cuentas adicionales")
    public List<FondoDTO> listar() {
        return service.listar();
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial de caja", description = "Lista movimientos de caja filtrables por fecha, moneda y tipo")
    public List<MovimientoFondoDTO> historial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) String tipo) {
        return service.historial(fecha, moneda, tipo);
    }

    @GetMapping("/historial/pagina")
    @Operation(summary = "Historial de caja paginado", description = "Lista movimientos de caja con paginacion y filtros opcionales")
    public Page<MovimientoFondoDTO> historialPaginado(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) String moneda,
            @RequestParam(required = false) String tipo,
            @PageableDefault(size = 25) Pageable pageable) {
        return service.historialPaginado(fecha, moneda, tipo, pageable);
    }

    @PutMapping("/{moneda}")
    @Operation(summary = "Ajustar fondo", description = "Ajusta manualmente el saldo de un fondo y deja movimiento de caja")
    public FondoDTO ajustar(@PathVariable String moneda, @Valid @RequestBody AjusteCapitalRequest req) {
        return service.ajustar(moneda, req);
    }

    @PostMapping("/entrada")
    @Operation(summary = "Registrar entrada", description = "Registra una entrada directa de capital a un fondo")
    public FondoDTO registrarEntrada(@Valid @RequestBody EntradaFondoRequest req) {
        return service.registrarEntrada(req);
    }

    @PostMapping("/transferencia")
    @Operation(summary = "Transferir entre fondos", description = "Genera salida y entrada trazables entre fondos")
    public void registrarTransferencia(@Valid @RequestBody TransferenciaFondoRequest req) {
        service.registrarTransferencia(req);
    }

    @GetMapping("/nequi/estado-mes")
    public EstadoNequiMesDTO estadoNequiMes(@RequestParam(required = false) String fondo) {
        return service.estadoNequiMes(fondo);
    }

    @PostMapping("/nequi/cuentas")
    public FondoDTO crearCuentaNequi(@RequestBody Map<String, String> body) {
        return service.crearCuentaNequi(body.get("alias"));
    }

    @DeleteMapping("/nequi/cuentas/{moneda}")
    public ResponseEntity<Void> eliminarCuentaNequi(@PathVariable String moneda) {
        service.eliminarCuentaNequi(moneda);
        return ResponseEntity.noContent().build();
    }
}
