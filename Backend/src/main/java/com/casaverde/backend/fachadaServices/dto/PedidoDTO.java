package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long id;
    private String nombre;
    private Long salonId;
    private String salonNombre;
    private String mesa;
    private String fechaReserva;
    private String horaReserva;
    private Integer cantPersonas;
    private String estado;
    private String observacion;
    private Boolean paraLlevar;
    private String telefono;
    private boolean enCurso;
    private BigDecimal subtotalCop;
    private BigDecimal descuentoCop;
    private String descuentoAplicadoPor;
    private BigDecimal totalCop;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private BigDecimal valorCambio;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;
    private LocalDateTime finalizadoEn;
    private LocalDateTime fiadoEn;
    private List<PedidoDetalleDTO> detalles;
    private List<PagoDTO> pagos;
}
