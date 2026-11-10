package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PagoEmpleadoRequest {
    @NotBlank(message = "La moneda es obligatoria")
    private String moneda;
    private LocalDate periodoInicio;
    private LocalDate periodoFin;
    private String observacion;
}
