package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjusteCapitalRequest {
    @NotNull(message = "El nuevo saldo es obligatorio")
    @PositiveOrZero(message = "El saldo no puede ser negativo")
    private BigDecimal nuevoSaldo;
    private String motivo;
}
