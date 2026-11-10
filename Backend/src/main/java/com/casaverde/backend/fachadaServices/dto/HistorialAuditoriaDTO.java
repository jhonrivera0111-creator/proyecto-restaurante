package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialAuditoriaDTO {
    private Long id;
    private String entidad;
    private Long entidadId;
    private String accion;
    private String detalle;
    private String usuario;
    private LocalDateTime registradoEn;
}
