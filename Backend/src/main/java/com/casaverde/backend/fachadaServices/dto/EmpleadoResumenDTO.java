package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoResumenDTO {
    private EmpleadoDTO empleado;
    private List<AdelantoDTO> adelantosPendientes;
    private List<TrabajoDiaDTO> diasPendientes;
    private BigDecimal totalAdelantosCop;
    private Integer totalDiasPendientes;
    private BigDecimal salarioBrutoEstimado;
    private BigDecimal netoEstimado;
}
