package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoEmpleado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoDTO {
    private Long id;
    private String nombre;
    private TipoEmpleado tipo;
    private BigDecimal salario;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
