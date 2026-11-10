package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearPedidoRequest {
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String nombre;
    private Long salonId;
    private String mesa;
    private String fechaReserva;
    private String horaReserva;
    private Integer cantPersonas;
    // Mismo máximo (30) que la columna pedido.telefono (varchar(30)) y que el
    // maxLength del input en el frontend. El Pattern valida el juego de
    // caracteres; @Size valida la longitud — se separan para mensajes claros.
    @Pattern(regexp = "^[+]?[0-9][0-9 \\-]{5,}$", message = "Teléfono inválido")
    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    private String telefono;
    @Positive(message = "El abono debe ser mayor a 0")
    private BigDecimal abono;
    private String monedaAbono;
    private String observacion;
    private Boolean paraLlevar;
    @PositiveOrZero(message = "El descuento no puede ser negativo")
    private BigDecimal descuentoCop;
    @Valid
    private List<DetalleItemRequest> detalles;
}
