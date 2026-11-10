package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCuentaCobrar;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuentaCobrarDTO {
    private Long id;
    private String clienteNombre;
    private String concepto;
    private BigDecimal monto;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private EstadoCuentaCobrar estado;
    private String celular;
    private LocalDate fecha;
    private String observacion;
    private LocalDateTime creadoEn;
    private List<AbonoCuentaCobrarDTO> abonos;
}
