package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenDiaDTO {

    private String fecha; // yyyy-MM-dd

    private long pedidosTotales;
    private long pedidosAbiertos;
    private long pedidosFinalizados;
    private long pedidosAnulados;

    // Animales vendidos (pedidos finalizados en la fecha, nunca anulados/pendientes/
    // reservas futuras). Misma unidad fraccionada que usa inventario (0.5 = medio, etc.)
    private BigDecimal cuyesVendidos;
    private BigDecimal gallinasVendidas;

    private BigDecimal cobradoCop;
    private BigDecimal cobradoUsd;
    private BigDecimal cobradoNequi;
    private BigDecimal cobradoTotalCop;

    private BigDecimal gastosCop;
    private BigDecimal gastosUsd;
    private BigDecimal gastosNequi;
    private BigDecimal gastosTotalCop;

    private BigDecimal balanceNeto;

    private List<FondoDTO> fondos;
    private List<PedidoDTO> pedidos;
    private List<GastoDTO> gastos;
}
