package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.time.LocalDate;

import com.casaverde.backend.fachadaServices.dto.ResumenDiaDTO;

public interface IResumenService {

    ResumenDiaDTO obtener(LocalDate fecha);
}
