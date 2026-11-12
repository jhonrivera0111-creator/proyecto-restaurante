package com.casaverde.backend.fachadaServices.services.cajaService;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CajaMovimiento;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Fondo;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.capaAccesoADatos.repositories.CajaMovimientoRepository;
import com.casaverde.backend.capaAccesoADatos.repositories.FondoRepository;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;
import com.casaverde.backend.fachadaServices.services.configuracionService.ITasaCambioService;

import lombok.RequiredArgsConstructor;

/**
 * LIBRO DE CAJA: único punto por el que cambia el saldo de un fondo.
 * Todo ingreso o egreso de dinero (cobro de pedido, gasto, compra, nómina,
 * transferencia, ajuste...) pasa por registrar(). Así cada cambio de saldo
 * deja una fila inmutable en CajaMovimiento con el saldo resultante y su
 * origen (referenciaTipo + referenciaId): se puede reconstruir cualquier saldo.
 */
@Service
@RequiredArgsConstructor
public class CajaMovimientoServiceImpl implements ICajaMovimientoService {

        private final FondoRepository fondoRepo;
        private final CajaMovimientoRepository cajaMovimientoRepo;
        private final ITasaCambioService tasaCambioService;
        private final IMonetaryService monetaryService;

        /**
         * Aplica un movimiento a un fondo. monto > 0 = entra dinero, monto < 0 = sale.
         * Reglas: no acepta movimientos de 0; un egreso NO puede dejar el fondo en
         * negativo (se rechaza con ValidacionException); guarda el saldo resultante.
         * MANDATORY: exige estar dentro de la transacción de quien lo llama, para
         * que el pedido/gasto y su asiento de caja se confirmen o fallen juntos.
         */
        @Transactional(propagation = Propagation.MANDATORY)
        public CajaMovimiento registrar(
                        CajaMovimientoTipo tipo,
                        String moneda,
                        BigDecimal monto,
                        BigDecimal montoCop,
                        String referenciaTipo,
                        Long referenciaId,
                        String concepto,
                        String observacion) {

                String monedaNormalizada = monetaryService.normalizarMoneda(moneda);
                BigDecimal montoNormalizado = monetaryService.normalizarSaldoFondo(monedaNormalizada, monto);
                if (montoNormalizado.compareTo(BigDecimal.ZERO) == 0) {
                        throw new ValidacionException("El movimiento de caja no puede ser cero");
                }

                BigDecimal montoCopNormalizado = montoCop != null
                                ? montoCop.setScale(0, java.math.RoundingMode.HALF_UP)
                                : monetaryService.convertirACop(monedaNormalizada, montoNormalizado,
                                                tasaCambioService.getValorVigente());

                Fondo fondo = fondoRepo.findById(monedaNormalizada)
                                .orElseGet(() -> Fondo.builder().moneda(monedaNormalizada).saldo(BigDecimal.ZERO)
                                                .build());
                BigDecimal saldoResultante = fondo.getSaldo().add(montoNormalizado);
                if (saldoResultante.compareTo(BigDecimal.ZERO) < 0) {
                        throw new ValidacionException("El fondo " + monedaNormalizada
                                        + " no tiene saldo suficiente. Disponible: " + fondo.getSaldo()
                                        + ", movimiento: " + montoNormalizado.abs());
                }
                fondo.setSaldo(saldoResultante);
                fondoRepo.save(fondo);

                return cajaMovimientoRepo.save(CajaMovimiento.builder()
                                .tipo(tipo)
                                .moneda(monedaNormalizada)
                                .monto(montoNormalizado)
                                .montoCop(montoCopNormalizado)
                                .saldoResultante(saldoResultante)
                                .referenciaTipo(referenciaTipo)
                                .referenciaId(referenciaId)
                                .concepto(concepto)
                                .observacion(observacion)
                                .build());
        }

}
