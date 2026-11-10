package com.casaverde.backend.fachadaServices.services.productoService;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.casaverde.backend.config.MonedaConstants;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;

@Service
public class MonetaryServiceImpl implements IMonetaryService {

    public String normalizarMoneda(String moneda) {
        if (moneda == null || moneda.isBlank()) {
            throw new ValidacionException("La moneda es obligatoria");
        }
        String normalizada = moneda.trim().toUpperCase();
        if (!normalizada.equals(MonedaConstants.COP) && !normalizada.equals(MonedaConstants.USD)
                && !normalizada.equals(MonedaConstants.EFECTIVO) && !normalizada.startsWith(MonedaConstants.NEQUI)) {
            throw new ValidacionException("Moneda invalida: " + moneda + ". Use COP, USD, EFECTIVO o NEQUI");
        }
        return normalizada;
    }

    public BigDecimal normalizarMontoEntrada(String moneda, BigDecimal monto) {
        validarPositivo(monto, "El monto debe ser mayor a 0");
        return normalizarEscala(normalizarMoneda(moneda), monto);
    }

    public BigDecimal normalizarSaldoFondo(String moneda, BigDecimal saldo) {
        if (saldo == null) {
            throw new ValidacionException("El saldo es obligatorio");
        }
        return normalizarEscala(normalizarMoneda(moneda), saldo);
    }

    public BigDecimal normalizarPrecioCop(BigDecimal precio) {
        if (precio == null) {
            return BigDecimal.ZERO;
        }
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El precio debe ser mayor o igual a 0");
        }
        return exigirEscalaMaxima(precio, 0, "Los precios en COP no deben tener centavos").setScale(0,
                RoundingMode.UNNECESSARY);
    }

    public BigDecimal normalizarTasa(BigDecimal tasa) {
        validarPositivo(tasa, "La tasa debe ser mayor a 0");
        return tasa.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    public BigDecimal convertirACop(String moneda, BigDecimal monto, BigDecimal tasaUsd) {
        String normalizada = normalizarMoneda(moneda);
        BigDecimal convertido = MonedaConstants.USD.equals(normalizada) ? monto.multiply(tasaUsd) : monto;
        return convertido.setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizarEscala(String moneda, BigDecimal monto) {
        if (MonedaConstants.USD.equals(moneda))
            return exigirEscalaMaxima(monto, 2, "USD admite maximo 2 decimales").setScale(2, RoundingMode.UNNECESSARY);
        if (MonedaConstants.COP.equals(moneda) || MonedaConstants.EFECTIVO.equals(moneda) || moneda.startsWith(MonedaConstants.NEQUI))
            return exigirEscalaMaxima(monto, 0, moneda + " no debe tener centavos").setScale(0,
                    RoundingMode.UNNECESSARY);
        throw new ValidacionException("Moneda invalida: " + moneda + ". Use COP, USD, EFECTIVO o NEQUI");
    }

    private void validarPositivo(BigDecimal valor, String mensaje) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException(mensaje);
        }
    }

    private BigDecimal exigirEscalaMaxima(BigDecimal valor, int escalaMaxima, String mensaje) {
        BigDecimal limpio = valor.stripTrailingZeros();
        int escala = Math.max(limpio.scale(), 0);
        if (escala > escalaMaxima) {
            throw new ValidacionException(mensaje);
        }
        return limpio;
    }
}
