package com.casaverde.backend.fachadaServices.services.configuracionService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.ConfiguracionDTO;

public interface IConfiguracionGeneralService {

    List<ConfiguracionDTO> listarTodas();

    ConfiguracionDTO buscarPorClave(String clave);

    ConfiguracionDTO guardar(ConfiguracionDTO dto);

    ConfiguracionDTO actualizarValor(String clave, String valor);

    void eliminar(String clave);
}
