package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPedidoRequest {
    private String nombre;
    private String fechaReserva; // null = quitar fecha
    private String horaReserva; // null = quitar hora
    private String mesa;
    private Integer cantPersonas;
    private String observacion;
    private Boolean paraLlevar;
    private Long salonId; // null = sin salón
    // Mismo máximo (30) que CrearPedidoRequest.telefono, la columna pedido.telefono
    // y el maxLength del frontend.
    @Pattern(regexp = "^[+]?[0-9][0-9 \\-]{5,}$", message = "Teléfono inválido")
    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefono; // null = quitar teléfono
    // null = no modificar el descuento actual. Solo editable sin pagos
    // registrados (PedidoServiceImpl.actualizarPedido lo valida).
    @PositiveOrZero(message = "El descuento no puede ser negativo")
    private BigDecimal descuentoCop;
}
