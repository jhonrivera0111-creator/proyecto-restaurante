package com.casaverde.backend.fachadaServices.services.pedidoService;

import com.casaverde.backend.fachadaServices.dto.PedidoDTO;

public interface IComprobanteService {
    String generar(PedidoDTO dto);
}
