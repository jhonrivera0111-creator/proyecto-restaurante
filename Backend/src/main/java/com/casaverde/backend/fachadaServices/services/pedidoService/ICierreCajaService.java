package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.time.LocalDate;
import java.util.List;

import com.casaverde.backend.fachadaServices.dto.CierreCajaDTO;
import com.casaverde.backend.fachadaServices.dto.CierreCajaRequest;

public interface ICierreCajaService {

    List<CierreCajaDTO> listar();

    CierreCajaDTO obtener(LocalDate fecha);

    CierreCajaDTO cerrar(CierreCajaRequest request);
}
