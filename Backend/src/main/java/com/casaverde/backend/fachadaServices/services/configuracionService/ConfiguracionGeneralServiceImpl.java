package com.casaverde.backend.fachadaServices.services.configuracionService;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.models.entitys.ConfiguracionGeneral;
import com.casaverde.backend.fachadaServices.dto.ConfiguracionDTO;
import com.casaverde.backend.config.controllerAdvice.RecursoNoEncontradoException;
import com.casaverde.backend.config.controllerAdvice.ValidacionException;
import com.casaverde.backend.fachadaServices.mapper.ConfiguracionMapper;
import com.casaverde.backend.capaAccesoADatos.repositories.ConfiguracionGeneralRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionGeneralServiceImpl implements IConfiguracionGeneralService {

    private final ConfiguracionGeneralRepository repo;
    private final ConfiguracionMapper configuracionMapper;

    @Transactional(readOnly = true)
    public List<ConfiguracionDTO> listarTodas() {
        return repo.findAll().stream().map(configuracionMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ConfiguracionDTO buscarPorClave(String clave) {
        return configuracionMapper.toDTO(findOrThrow(clave));
    }

    @Transactional
    public ConfiguracionDTO guardar(ConfiguracionDTO dto) {
        if (dto.getClave() == null || dto.getClave().isBlank()) {
            throw new ValidacionException("La clave de configuración es obligatoria");
        }
        ConfiguracionGeneral config = repo.findById(dto.getClave())
                .orElse(new ConfiguracionGeneral());
        config.setClave(dto.getClave().trim());
        config.setValor(dto.getValor() != null ? dto.getValor() : "");
        config.setDescripcion(dto.getDescripcion());
        return configuracionMapper.toDTO(repo.save(config));
    }

    @Transactional
    public ConfiguracionDTO actualizarValor(String clave, String valor) {
        ConfiguracionGeneral config = findOrThrow(clave);
        config.setValor(valor != null ? valor : "");
        return configuracionMapper.toDTO(repo.save(config));
    }

    @Transactional
    public void eliminar(String clave) {
        if (!repo.existsById(clave)) {
            throw new RecursoNoEncontradoException("Configuración no encontrada: " + clave);
        }
        repo.deleteById(clave);
    }

    private ConfiguracionGeneral findOrThrow(String clave) {
        return repo.findById(clave)
                .orElseThrow(() -> new RecursoNoEncontradoException("Configuración no encontrada: " + clave));
    }

}
