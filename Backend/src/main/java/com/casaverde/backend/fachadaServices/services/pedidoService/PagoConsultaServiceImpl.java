package com.casaverde.backend.fachadaServices.services.pedidoService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.casaverde.backend.capaAccesoADatos.repositories.PagoRepository;
import com.casaverde.backend.fachadaServices.dto.PagoDTO;
import com.casaverde.backend.fachadaServices.mapper.PedidoMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PagoConsultaServiceImpl implements IPagoConsultaService {

    private final PagoRepository pagoRepo;
    private final PedidoMapper pedidoMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<PagoDTO> listarPaginado(Pageable pageable) {
        return pagoRepo.findAllByOrderByRegistradoEnDesc(pageable)
                .map(pedidoMapper::toPagoDTO);
    }
}
