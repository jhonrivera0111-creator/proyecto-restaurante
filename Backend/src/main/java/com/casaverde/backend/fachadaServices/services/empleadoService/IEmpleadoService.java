package com.casaverde.backend.fachadaServices.services.empleadoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.AdelantoDTO;
import com.casaverde.backend.fachadaServices.dto.AdelantoRequest;
import com.casaverde.backend.fachadaServices.dto.EmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.EmpleadoRequest;
import com.casaverde.backend.fachadaServices.dto.EmpleadoResumenDTO;
import com.casaverde.backend.fachadaServices.dto.PagoEmpleadoDTO;
import com.casaverde.backend.fachadaServices.dto.PagoEmpleadoRequest;
import com.casaverde.backend.fachadaServices.dto.TrabajoDiaDTO;
import com.casaverde.backend.fachadaServices.dto.TrabajoDiaRequest;
import com.casaverde.backend.fachadaServices.dto.VacacionDTO;
import com.casaverde.backend.fachadaServices.dto.VacacionRequest;

public interface IEmpleadoService {

    List<EmpleadoDTO> listar(boolean soloActivos);
    EmpleadoDTO crear(EmpleadoRequest req);
    EmpleadoDTO actualizar(Long id, EmpleadoRequest req);
    EmpleadoDTO toggle(Long id);

    List<AdelantoDTO> listarAdelantos(Long empleadoId);
    AdelantoDTO registrarAdelanto(Long empleadoId, AdelantoRequest req);

    List<TrabajoDiaDTO> listarDias(Long empleadoId, boolean soloNoPagados);
    TrabajoDiaDTO registrarDia(Long empleadoId, TrabajoDiaRequest req);
    void eliminarDia(Long empleadoId, Long diaId);

    List<PagoEmpleadoDTO> listarPagos(Long empleadoId);
    EmpleadoResumenDTO previewPago(Long empleadoId);
    PagoEmpleadoDTO liquidar(Long empleadoId, PagoEmpleadoRequest req);

    List<VacacionDTO> listarVacaciones(Long empleadoId);
    VacacionDTO registrarVacacion(Long empleadoId, VacacionRequest req);
    void eliminarVacacion(Long empleadoId, Long vacId);
}
