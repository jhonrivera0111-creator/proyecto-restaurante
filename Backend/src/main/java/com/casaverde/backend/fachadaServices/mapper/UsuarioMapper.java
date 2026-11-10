package com.casaverde.backend.fachadaServices.mapper;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Usuario;
import com.casaverde.backend.fachadaServices.dto.UsuarioDTO;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
    @Mapping(target = "token", ignore = true)
    UsuarioDTO toDTO(Usuario u);
}
