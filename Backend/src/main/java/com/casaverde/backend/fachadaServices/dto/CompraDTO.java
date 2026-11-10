package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompraDTO {
    private Long id;
    private Long proveedorId;
    private String proveedorNombre;
    private String insumo;
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal totalCop;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private EstadoCompra estado;
    private String observacion;
    private LocalDate fecha;
    private LocalDateTime creadoEn;
    private List<AbonoCompraDTO> abonos;
}
