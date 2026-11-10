package com.casaverde.backend.fachadaServices.services.configuracionService;

import java.math.BigDecimal;
import java.util.List;

import com.casaverde.backend.fachadaServices.dto.TasaCambioDTO;

public interface ITasaCambioService {

    TasaCambioDTO obtenerVigente();

    List<TasaCambioDTO> historial();

    TasaCambioDTO registrar(BigDecimal valorCop);

    BigDecimal getValorVigente();
}
