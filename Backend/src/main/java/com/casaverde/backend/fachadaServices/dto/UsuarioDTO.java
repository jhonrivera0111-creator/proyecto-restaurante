package com.casaverde.backend.fachadaServices.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private Long id;
    private String nombre;
    private String rol;
    private Boolean requierePassword;
    private Boolean activo;
    private String token;
}
