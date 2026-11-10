package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GastoRequest {
    @NotBlank(message = "El concepto del gasto es obligatorio")
    private String concepto;
    private String categoria;
    @NotBlank(message = "La moneda es obligatoria")
    private String moneda;
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    private String insumoInventario;
    private BigDecimal cantidadInventario;
    private String observacion;
}
