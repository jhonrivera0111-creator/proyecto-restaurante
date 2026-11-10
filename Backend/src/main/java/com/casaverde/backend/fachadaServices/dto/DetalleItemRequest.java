package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleItemRequest {
    private Long productoId;
    private String nombreManual;
    private BigDecimal precioManual;
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @AssertTrue(message = "Debe especificar productoId o nombreManual, no ambos ni ninguno")
    public boolean isProductoOManualValido() {
        boolean tieneProducto = productoId != null;
        boolean tieneManual   = nombreManual != null && !nombreManual.isBlank();
        return tieneProducto ^ tieneManual;
    }
}
