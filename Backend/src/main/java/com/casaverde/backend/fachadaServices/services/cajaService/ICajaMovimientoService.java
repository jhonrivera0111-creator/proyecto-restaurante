package com.casaverde.backend.fachadaServices.services.cajaService;

import java.math.BigDecimal;

import com.casaverde.backend.capaAccesoADatos.models.entitys.CajaMovimiento;
import com.casaverde.backend.capaAccesoADatos.models.enums.CajaMovimientoTipo;

public interface ICajaMovimientoService {

    /**
     * Registra un movimiento de caja y ajusta el saldo del fondo correspondiente.
     * <p>
     * <strong>Precondición:</strong> debe existir una transacción activa en el hilo
     * que invoca este método ({@code Propagation.MANDATORY}). Si no la hay, se lanza
     * {@link org.springframework.transaction.IllegalTransactionStateException}.
     * Llamar siempre desde un método anotado con {@code @Transactional}.
     * </p>
     */
    CajaMovimiento registrar(
            CajaMovimientoTipo tipo,
            String moneda,
            BigDecimal monto,
            BigDecimal montoCop,
            String referenciaTipo,
            Long referenciaId,
            String concepto,
            String observacion);
}
