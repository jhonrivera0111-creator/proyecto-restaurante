package com.casaverde.backend.fachadaServices.services.pedidoService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.casaverde.backend.fachadaServices.dto.PagoDTO;

public interface IPagoConsultaService {

    Page<PagoDTO> listarPaginado(Pageable pageable);
}
