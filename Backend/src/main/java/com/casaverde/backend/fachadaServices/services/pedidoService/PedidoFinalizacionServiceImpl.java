package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Pago;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Pedido;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.fachadaServices.services.cajaService.ICajaMovimientoService;
import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.fachadaServices.services.inventarioService.IInventarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoFinalizacionServiceImpl implements IPedidoFinalizacionService {

    private final IInventarioService inventarioService;
    private final ICajaMovimientoService cajaMovimientoService;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void ejecutar(Pedido pedido, BigDecimal totalCop) {
        inventarioService.descontarPorPedido(pedido);
        registrarCobroFinal(pedido, totalCop);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void ejecutarSoloCobro(Pedido pedido, BigDecimal totalCop) {
        registrarCobroFinal(pedido, totalCop);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void ejecutarSoloInventario(Pedido pedido) {
        inventarioService.descontarPorPedido(pedido);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void revertirInventario(Pedido pedido) {
        inventarioService.revertirPorPedido(pedido);
    }

    /**
     * Registra un único movimiento al fondo por cada moneda usada en el pedido.
     * Solo ingresa el valor del pedido (totalCop); el cambio nunca toca el fondo.
     */
    private void registrarCobroFinal(Pedido pedido, BigDecimal totalCop) {
        Map<String, BigDecimal[]> porMoneda = new LinkedHashMap<>();
        for (Pago p : pedido.getPagos()) {
            porMoneda.compute(p.getMoneda(), (k, arr) -> {
                if (arr == null) arr = new BigDecimal[]{ BigDecimal.ZERO, BigDecimal.ZERO };
                arr[0] = arr[0].add(p.getMonto());
                arr[1] = arr[1].add(p.getMontoCop());
                return arr;
            });
        }

        BigDecimal restante = totalCop;
        for (Map.Entry<String, BigDecimal[]> e : porMoneda.entrySet()) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;
            String moneda = e.getKey();
            BigDecimal montoOrig   = e.getValue()[0];
            BigDecimal montoCopTot = e.getValue()[1];

            BigDecimal montoCopFondo = montoCopTot.min(restante);
            BigDecimal montoFondo;
            if (MonedaConstants.USD.equals(moneda) && montoCopTot.compareTo(BigDecimal.ZERO) > 0) {
                montoFondo = montoOrig.multiply(montoCopFondo)
                        .divide(montoCopTot, 2, RoundingMode.FLOOR);
            } else {
                montoFondo = montoCopFondo;
            }

            if (montoFondo.compareTo(BigDecimal.ZERO) > 0) {
                // Los cobros en efectivo (COP) se acumulan en la caja física
                // "EFECTIVO", no en el capital neto acumulado — ese solo crece
                // cuando se consigna. USD y NEQUI siguen acreditando su propio
                // fondo directamente (no son billetes en caja).
                String fondoDestino = MonedaConstants.COP.equals(moneda) ? MonedaConstants.EFECTIVO : moneda;
                cajaMovimientoService.registrar(
                        CajaMovimientoTipo.COBRO_PEDIDO,
                        fondoDestino,
                        montoFondo,
                        montoCopFondo,
                        "pedido",
                        pedido.getId(),
                        "Cobro pedido #" + pedido.getId(),
                        null);
                restante = restante.subtract(montoCopFondo);
            }
        }
    }
}
