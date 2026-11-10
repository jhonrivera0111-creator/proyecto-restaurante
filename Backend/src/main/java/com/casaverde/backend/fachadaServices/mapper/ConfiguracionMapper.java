package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.ConfiguracionGeneral;
import com.casaverde.backend.fachadaServices.dto.ConfiguracionDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConfiguracionMapper {
    ConfiguracionDTO toDTO(ConfiguracionGeneral c);
}
