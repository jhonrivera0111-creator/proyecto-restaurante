package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;

import com.casaverde.backend.fachadaServices.dto.ActualizarDetalleRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPagoRequest;
import com.casaverde.backend.fachadaServices.dto.ActualizarPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.CrearPedidoRequest;
import com.casaverde.backend.fachadaServices.dto.DetalleItemRequest;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoHistoricoRequest;
import com.casaverde.backend.fachadaServices.dto.RegistrarPagoRequest;
import com.casaverde.backend.fachadaServices.services.pedidoService.IPedidoService;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Operacion principal de pedidos, pagos, comprobantes y cierre operativo")
public class PedidoController {

    private final IPedidoService service;

    @GetMapping
    @Operation(summary = "Listar pedidos", description = "Lista pedidos activos o todo el historial cuando todos=true")
    public List<PedidoDTO> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return todos ? service.listarTodos() : service.listarActivos();
    }

    @GetMapping("/pagina")
    @Operation(summary = "Listar historial paginado", description = "Lista todos los pedidos con paginacion, ordenados por creacion descendente")
    public Page<PedidoDTO> listarPaginado(@PageableDefault(size = 25) Pageable pageable) {
        return service.listarTodosPaginado(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener pedido", description = "Consulta el detalle completo de un pedido")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public PedidoDTO obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping(value = "/{id}/comprobante", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Generar comprobante", description = "Devuelve el comprobante del pedido en texto plano")
    public String comprobante(@PathVariable Long id) {
        return service.comprobanteTexto(id);
    }

    @PostMapping
    @Operation(summary = "Crear pedido", description = "Crea un pedido con items y abono inicial opcional")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos")
    })
    public ResponseEntity<PedidoDTO> crear(@Valid @RequestBody CrearPedidoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PostMapping("/historico")
    @Operation(summary = "Cargar pedido histórico", description = "Crea, cobra y finaliza un pedido ya ocurrido en una sola operación, fechado retroactivamente — afecta fondos e inventario de verdad. Uso administrativo (empalme de registros previos al uso del sistema).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido histórico registrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    public ResponseEntity<PedidoDTO> crearHistorico(@Valid @RequestBody PedidoHistoricoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearHistorico(req));
    }

    @PatchMapping("/{id}")
    public PedidoDTO actualizarPedido(@PathVariable Long id, @Valid @RequestBody ActualizarPedidoRequest req) {
        return service.actualizarPedido(id, req);
    }

    @PostMapping("/{id}/detalles")
    public PedidoDTO agregarDetalle(@PathVariable Long id, @Valid @RequestBody DetalleItemRequest req) {
        return service.agregarDetalle(id, req);
    }

    @PatchMapping("/{id}/detalles/{detalleId}")
    public PedidoDTO actualizarDetalle(
            @PathVariable Long id,
            @PathVariable Long detalleId,
            @Valid @RequestBody ActualizarDetalleRequest req) {
        return service.actualizarDetalle(id, detalleId, req);
    }

    @DeleteMapping("/{id}/detalles/{detalleId}")
    public PedidoDTO quitarDetalle(@PathVariable Long id, @PathVariable Long detalleId) {
        return service.quitarDetalle(id, detalleId);
    }

    @PostMapping("/{id}/pagos")
    @Operation(summary = "Registrar pago", description = "Registra un pago del pedido sin acreditar fondos hasta finalizar")
    public PedidoDTO registrarPago(@PathVariable Long id, @Valid @RequestBody RegistrarPagoRequest req) {
        return service.registrarPago(id, req);
    }

    @DeleteMapping("/{id}/pagos/{pagoId}")
    public PedidoDTO quitarPago(@PathVariable Long id, @PathVariable Long pagoId) {
        return service.quitarPago(id, pagoId);
    }

    @PatchMapping("/{id}/pagos/{pagoId}")
    public PedidoDTO actualizarPago(
            @PathVariable Long id,
            @PathVariable Long pagoId,
            @Valid @RequestBody ActualizarPagoRequest req) {
        return service.actualizarPago(id, pagoId, req);
    }

    @PutMapping("/{id}/finalizar")
    @Operation(summary = "Finalizar pedido", description = "Finaliza un pedido pagado, descuenta inventario y acredita fondos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido finalizado"),
            @ApiResponse(responseCode = "400", description = "Pedido no pagado, sin items o con reserva futura")
    })
    public PedidoDTO finalizar(@PathVariable Long id) {
        return service.finalizarPedido(id);
    }

    @PutMapping("/{id}/anular")
    public PedidoDTO anular(@PathVariable Long id) {
        return service.anularPedido(id);
    }

    @PutMapping("/{id}/fiar")
    @Operation(summary = "Marcar como fiado", description = "Congela ítems, descuenta inventario y deja el saldo pendiente como deuda en Cuentas por Cobrar, sin acreditar fondo")
    public PedidoDTO marcarFiado(@PathVariable Long id) {
        return service.marcarFiado(id);
    }
}
