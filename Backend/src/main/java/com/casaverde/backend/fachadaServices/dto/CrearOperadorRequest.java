package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearOperadorRequest {
    @NotBlank(message = "El nombre del operador es obligatorio")
    private String nombre;
}
