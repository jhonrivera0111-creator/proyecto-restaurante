package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoEmpleado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EmpleadoRequest {
    private String nombre;
    private TipoEmpleado tipo;
    private BigDecimal salario;
}
