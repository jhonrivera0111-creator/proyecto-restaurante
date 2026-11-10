package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import com.casaverde.backend.fachadaServices.dto.AdelantoDTO;
import com.casaverde.backend.fachadaServices.dto.AdelantoRequest;
import com.casaverde.backend.fachadaServices.dto.EmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.EmpleadoRequest;
import com.casaverde.backend.fachadaServices.dto.EmpleadoResumenDTO;
import com.casaverde.backend.fachadaServices.dto.PagoEmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.PagoEmpleadoRequest;
import com.casaverde.backend.fachadaServices.dto.TrabajoDiaDTO;
import com.casaverde.backend.fachadaServices.dto.TrabajoDiaRequest;
import com.casaverde.backend.fachadaServices.dto.VacacionDTO;
import com.casaverde.backend.fachadaServices.dto.VacacionRequest;
import com.casaverde.backend.fachadaServices.services.empleadoService.IEmpleadoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/empleados")
@RequiredArgsConstructor
public class EmpleadoController {

    private final IEmpleadoService empleadoService;

    @GetMapping
    public List<EmpleadoDTO> listar(
            @RequestParam(defaultValue = "true") boolean soloActivos) {
        return empleadoService.listar(soloActivos);
    }

    @PostMapping
    public ResponseEntity<EmpleadoDTO> crear(@RequestBody EmpleadoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empleadoService.crear(req));
    }

    @PutMapping("/{id}")
    public EmpleadoDTO actualizar(@PathVariable Long id, @RequestBody EmpleadoRequest req) {
        return empleadoService.actualizar(id, req);
    }

    @PutMapping("/{id}/toggle")
    public EmpleadoDTO toggle(@PathVariable Long id) {
        return empleadoService.toggle(id);
    }

    // ─── Adelantos ────────────────────────────────────────────────────────

    @GetMapping("/{id}/adelantos")
    public List<AdelantoDTO> listarAdelantos(@PathVariable Long id) {
        return empleadoService.listarAdelantos(id);
    }

    @PostMapping("/{id}/adelantos")
    public ResponseEntity<AdelantoDTO> registrarAdelanto(
            @PathVariable Long id, @RequestBody @Valid AdelantoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.registrarAdelanto(id, req));
    }

    // ─── Días trabajados ──────────────────────────────────────────────────

    @GetMapping("/{id}/dias")
    public List<TrabajoDiaDTO> listarDias(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean soloNoPagados) {
        return empleadoService.listarDias(id, soloNoPagados);
    }

    @PostMapping("/{id}/dias")
    public ResponseEntity<TrabajoDiaDTO> registrarDia(
            @PathVariable Long id, @RequestBody TrabajoDiaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.registrarDia(id, req));
    }

    @DeleteMapping("/{id}/dias/{diaId}")
    public ResponseEntity<Void> eliminarDia(
            @PathVariable Long id, @PathVariable Long diaId) {
        empleadoService.eliminarDia(id, diaId);
        return ResponseEntity.noContent().build();
    }

    // ─── Pagos ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/pagos")
    public List<PagoEmpleadoDTO> listarPagos(@PathVariable Long id) {
        return empleadoService.listarPagos(id);
    }

    @GetMapping("/{id}/pagos/preview")
    public EmpleadoResumenDTO previewPago(@PathVariable Long id) {
        return empleadoService.previewPago(id);
    }

    @PostMapping("/{id}/pagos")
    public ResponseEntity<PagoEmpleadoDTO> liquidar(
            @PathVariable Long id, @RequestBody @Valid PagoEmpleadoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.liquidar(id, req));
    }

    // ─── Vacaciones ───────────────────────────────────────────────────────

    @GetMapping("/{id}/vacaciones")
    public List<VacacionDTO> listarVacaciones(@PathVariable Long id) {
        return empleadoService.listarVacaciones(id);
    }

    @PostMapping("/{id}/vacaciones")
    public ResponseEntity<VacacionDTO> registrarVacacion(
            @PathVariable Long id, @RequestBody VacacionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(empleadoService.registrarVacacion(id, req));
    }

    @DeleteMapping("/{id}/vacaciones/{vacId}")
    public ResponseEntity<Void> eliminarVacacion(
            @PathVariable Long id, @PathVariable Long vacId) {
        empleadoService.eliminarVacacion(id, vacId);
        return ResponseEntity.noContent().build();
    }
}
