package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TrabajoDiaRequest {
    private LocalDate fecha;
    private String observacion;
}
