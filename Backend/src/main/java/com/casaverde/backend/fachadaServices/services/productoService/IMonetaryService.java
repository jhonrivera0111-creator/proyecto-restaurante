package com.casaverde.backend.fachadaServices.services.productoService;

import java.math.BigDecimal;

public interface IMonetaryService {

    String normalizarMoneda(String moneda);

    BigDecimal normalizarMontoEntrada(String moneda, BigDecimal monto);

    BigDecimal normalizarSaldoFondo(String moneda, BigDecimal saldo);

    BigDecimal normalizarPrecioCop(BigDecimal precio);

    BigDecimal normalizarTasa(BigDecimal tasa);

    BigDecimal convertirACop(String moneda, BigDecimal monto, BigDecimal tasaUsd);
}
