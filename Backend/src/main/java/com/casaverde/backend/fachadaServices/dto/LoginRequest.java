package com.casaverde.backend.fachadaServices.dto;

import jakarta.validation.constraints.NotNull;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotNull(message = "Seleccione un usuario")
    private Long usuarioId;
    private String password;
}
