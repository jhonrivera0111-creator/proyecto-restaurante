package com.casaverde.backend.fachadaServices.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.casaverde.backend.capaAccesoADatos.models.entitys.AbonoCuentaCobrar;
import com.casaverde.backend.capaAccesoADatos.models.entitys.CuentaCobrar;
import com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarDTO;
import com.casaverde.backend.fachadaServices.dto.CuentaCobrarDTO;

@Mapper(componentModel = "spring")
public interface CuentaCobrarMapper {

    @Mapping(target = "totalAbonado", ignore = true)
    @Mapping(target = "saldoPendiente", ignore = true)
    @Mapping(target = "abonos", ignore = true)
    CuentaCobrarDTO toDTO(CuentaCobrar c);

    @Mapping(source = "cuenta.id", target = "cuentaId")
    AbonoCuentaCobrarDTO toAbonoDTO(AbonoCuentaCobrar a);
}
