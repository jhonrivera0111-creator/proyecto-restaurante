package com.casaverde.backend.capaControladores;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.services.pedidoService.IPagoConsultaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
@Tag(name = "Pagos", description = "Consulta historica paginada de pagos registrados en pedidos")
public class PagoController {

    private final IPagoConsultaService service;

    @GetMapping("/historial/pagina")
    @Operation(summary = "Historial paginado de pagos", description = "Lista pagos de pedidos ordenados por fecha de registro descendente")
    public Page<PagoDTO> historialPaginado(@PageableDefault(size = 25) Pageable pageable) {
        return service.listarPaginado(pageable);
    }
}
