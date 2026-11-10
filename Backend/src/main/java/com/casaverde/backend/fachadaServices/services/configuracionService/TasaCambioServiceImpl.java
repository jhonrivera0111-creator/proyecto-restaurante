package com.casaverde.backend.fachadaServices.services.configuracionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.TasaCambio;
import com.casaverde.backend.fachadaServices.dto.TasaCambioDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.TasaCambioMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.TasaCambioRepository;
import com.casaverde.backend.fachadaServices.services.productoService.IAuditoriaService;
import com.casaverde.backend.fachadaServices.services.productoService.IMonetaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TasaCambioServiceImpl implements ITasaCambioService {

    private final TasaCambioRepository repo;
    private final IMonetaryService monetaryService;
    private final IAuditoriaService auditoriaService;
    private final TasaCambioMapper tasaCambioMapper;

    @Transactional(readOnly = true)
    public TasaCambioDTO obtenerVigente() {
        return repo.findVigente()
                .map(tasaCambioMapper::toDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay tasa de cambio registrada"));
    }

    @Transactional(readOnly = true)
    public List<TasaCambioDTO> historial() {
        return repo.findAllByOrderByVigenteDesdeDesc().stream()
                .map(tasaCambioMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = "tasa-vigente", allEntries = true)
    public TasaCambioDTO registrar(BigDecimal valorCop) {
        valorCop = monetaryService.normalizarTasa(valorCop);
        TasaCambio tasa = TasaCambio.builder().valorCop(valorCop).build();
        TasaCambio guardada = repo.save(tasa);
        auditoriaService.registrar("tasa_cambio", guardada.getId(), "REGISTRAR",
                "Nueva tasa USD/COP " + valorCop);
        return tasaCambioMapper.toDTO(guardada);
    }

    /**
     * Retorna el valor COP de la tasa vigente.
     * Lanza ValidacionException si no hay tasa configurada.
     * Usar en servicios que necesitan convertir USD a COP.
     */
    @Cacheable("tasa-vigente")
    @Transactional(readOnly = true)
    public BigDecimal getValorVigente() {
        return repo.findVigente()
                .orElseThrow(() -> new ValidacionException("No hay tasa de cambio configurada"))
                .getValorCop();
    }

}
