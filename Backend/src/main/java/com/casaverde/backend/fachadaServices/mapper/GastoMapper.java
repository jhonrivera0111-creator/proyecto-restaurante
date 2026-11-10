package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Gasto;
import com.casaverde.backend.fachadaServices.dto.GastoDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GastoMapper {
    GastoDTO toDTO(Gasto g);
}
