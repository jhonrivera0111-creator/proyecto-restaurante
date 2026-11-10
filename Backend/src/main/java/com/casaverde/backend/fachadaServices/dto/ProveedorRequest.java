package com.casaverde.backend.fachadaServices.dto;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoProveedor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    @NotNull(message = "El tipo de proveedor es obligatorio")
    private TipoProveedor tipo;
    private String telefono;
}
