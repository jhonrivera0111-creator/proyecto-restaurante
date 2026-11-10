package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.casaverde.backend.capaAccesoADatos.models.enums.TipoAdelanto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdelantoRequest {
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser mayor a 0")
    private BigDecimal monto;
    private String moneda;
    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;
    // observacion es obligatoria solo para tipo ESPECIE — se valida en el servicio
    private String observacion;
    private TipoAdelanto tipo;
}
