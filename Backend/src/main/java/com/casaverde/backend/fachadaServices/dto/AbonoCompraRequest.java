package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AbonoCompraRequest {
    // monto y moneda son opcionales cuando adelantoProveedorId está presente
    private BigDecimal monto;
    private String moneda;
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    private Long adelantoProveedorId;
}
