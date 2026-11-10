package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionDTO {
    private String clave;
    private String valor;
    private String descripcion;
    private LocalDateTime actualizadoEn;
}
