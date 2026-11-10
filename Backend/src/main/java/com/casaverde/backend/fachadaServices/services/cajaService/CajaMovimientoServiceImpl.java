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

@Service
@RequiredArgsConstructor
public class CajaMovimientoServiceImpl implements ICajaMovimientoService {

        private final FondoRepository fondoRepo;
        private final CajaMovimientoRepository cajaMovimientoRepo;
        private final ITasaCambioService tasaCambioService;
        private final IMonetaryService monetaryService;

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
