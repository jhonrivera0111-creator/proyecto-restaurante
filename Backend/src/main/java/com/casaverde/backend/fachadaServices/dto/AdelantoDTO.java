package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoAdelanto;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdelantoDTO {
    private Long id;
    private Long empleadoId;
    private TipoAdelanto tipo;
    private BigDecimal monto;
    private String moneda;
    private BigDecimal montoCop;
    private BigDecimal tasaUsada;
    private String fondoDebitado;
    private LocalDate fecha;
    private String observacion;
    private Boolean pagado;
    private LocalDateTime creadoEn;
}
