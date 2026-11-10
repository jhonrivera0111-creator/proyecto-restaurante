package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CuentaCobrarRequest {
    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String clienteNombre;
    private String concepto;
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    private String celular;
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    private String observacion;
}
