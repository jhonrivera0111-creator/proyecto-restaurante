package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrabajoDiaDTO {
    private Long id;
    private Long empleadoId;
    private LocalDate fecha;
    private String observacion;
    private Boolean pagado;
    private LocalDateTime creadoEn;
}
