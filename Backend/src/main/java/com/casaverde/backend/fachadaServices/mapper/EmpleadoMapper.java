package com.casaverde.backend.fachadaServices.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.casaverde.backend.capaAccesoADatos.models.entitys.Adelanto;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Empleado;
import com.casaverde.backend.capaAccesoADatos.models.entitys.PagoEmpleado;
import com.casaverde.backend.capaAccesoADatos.models.entitys.TrabajoDia;
import com.casaverde.backend.capaAccesoADatos.models.entitys.Vacacion;
import com.casaverde.backend.fachadaServices.dto.AdelantoDTO;
import com.casaverde.backend.fachadaServices.dto.EmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.PagoEmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.TrabajoDiaDTO;
import com.casaverde.backend.fachadaServices.dto.VacacionDTO;

@Mapper(componentModel = "spring")
public interface EmpleadoMapper {

    EmpleadoDTO toDTO(Empleado e);

    @Mapping(source = "empleado.id", target = "empleadoId")
    AdelantoDTO toAdelantoDTO(Adelanto a);

    @Mapping(source = "empleado.id", target = "empleadoId")
    TrabajoDiaDTO toTrabajoDiaDTO(TrabajoDia t);

    @Mapping(source = "empleado.id", target = "empleadoId")
    PagoEmpleadoDTO toPagoDTO(PagoEmpleado p);

    @Mapping(source = "empleado.id", target = "empleadoId")
    VacacionDTO toVacacionDTO(Vacacion v);
}
