package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VacacionDTO {
    private Long id;
    private Long empleadoId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observacion;
    private LocalDateTime creadoEn;
}
