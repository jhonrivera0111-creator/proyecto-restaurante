package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoRequest {
    @NotBlank(message = "El insumo es obligatorio")
    private String insumo;
    @NotBlank(message = "El tipo de movimiento es obligatorio")
    private String tipo;
    @NotNull(message = "La cantidad es obligatoria")
    @PositiveOrZero(message = "La cantidad debe ser mayor o igual a 0")
    private BigDecimal cantidad;
    private String observacion;
}
