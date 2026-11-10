package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.HistorialAuditoria;
import com.casaverde.backend.fachadaServices.dto.HistorialAuditoriaDTO;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditoriaMapper {
    HistorialAuditoriaDTO toDTO(HistorialAuditoria a);
}
