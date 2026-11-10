package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.TasaCambio;
import com.casaverde.backend.fachadaServices.dto.TasaCambioDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TasaCambioMapper {
    TasaCambioDTO toDTO(TasaCambio t);
}
