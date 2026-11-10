package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDateTime;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProveedorDTO {
    private Long id;
    private String nombre;
    private TipoProveedor tipo;
    private String telefono;
    private boolean activo;
    private LocalDateTime creadoEn;
}
