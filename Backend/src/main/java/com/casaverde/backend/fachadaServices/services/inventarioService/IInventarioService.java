package com.casaverde.backend.fachadaServices.services.inventarioService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.fachadaServices.dto.ComparativoMensualDTO;
import com.casaverde.backend.fachadaServices.dto.InventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoInventarioDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoRequest;

public interface IInventarioService {

    List<InventarioDTO> listarStock();

    InventarioDTO obtenerStock(String insumo);

    List<MovimientoInventarioDTO> listarMovimientos();

    List<MovimientoInventarioDTO> listarMovimientosPorInsumo(String insumo);

    Page<MovimientoInventarioDTO> listarMovimientosPaginado(String insumo, Pageable pageable);

    MovimientoInventarioDTO registrarMovimiento(MovimientoRequest req);

    void descontarPorPedido(Pedido pedido);

    /**
     * Revierte (suma de vuelta) el consumo de inventario de un pedido —
     * usado al anular un pedido en estado FIADO, cuyo inventario ya se
     * había descontado al marcarlo fiado.
     */
    void revertirPorPedido(Pedido pedido);

    /** Insumo -> cantidad requerida por el pedido, leída del composicionSnapshot de cada detalle. */
    Map<String, BigDecimal> calcularConsumos(Pedido pedido);

    InventarioDTO actualizarAlerta(String insumo, BigDecimal alertaMinima);

    ComparativoMensualDTO comparativoAnual(int año);
}
