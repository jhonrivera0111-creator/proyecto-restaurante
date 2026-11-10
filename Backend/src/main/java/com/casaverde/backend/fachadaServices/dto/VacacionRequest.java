package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VacacionRequest {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String observacion;
}
