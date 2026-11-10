package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdelantoProveedorDTO {
    private Long id;
    private Long proveedorId;
    private String proveedorNombreSnapshot;
    private BigDecimal monto;
    private BigDecimal saldoRestante;
    private BigDecimal saldoUsado;
    private String moneda;
    private BigDecimal montoCopInicial;
    private String fondoDebitado;
    private String insumo;
    private BigDecimal precioUnitarioAcordado;
    private LocalDate fecha;
    private String observacion;
    private Boolean activo;
    private LocalDateTime creadoEn;
}
