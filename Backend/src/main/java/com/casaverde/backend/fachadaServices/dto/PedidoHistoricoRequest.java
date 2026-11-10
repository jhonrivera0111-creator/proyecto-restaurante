package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.*;

/**
 * Carga retroactiva de un pedido que ya ocurrió en la realidad (venta previa
 * al uso de Casa Verde, o registrada fuera de fecha). A diferencia de
 * CrearPedidoRequest, el pedido se crea, cobra y finaliza en una sola
 * operación — no representa una reserva ni pasa por el ciclo normal
 * pendiente→pagado→finalizado en tiempo real.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoHistoricoRequest {
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotEmpty(message = "El pedido debe tener al menos un ítem")
    @Valid
    private List<DetalleItemRequest> detalles;

    @NotEmpty(message = "El pedido debe tener al menos un pago — se finaliza de inmediato")
    @Valid
    private List<RegistrarPagoRequest> pagos;

    @PositiveOrZero(message = "El descuento no puede ser negativo")
    private BigDecimal descuentoCop;

    private String observacion;
}
