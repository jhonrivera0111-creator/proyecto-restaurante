package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Salon;
import com.casaverde.backend.fachadaServices.dto.SalonDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SalonMapper {
    SalonDTO toDTO(Salon s);
}
