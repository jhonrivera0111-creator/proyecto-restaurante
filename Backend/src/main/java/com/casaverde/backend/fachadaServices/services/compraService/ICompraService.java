package com.casaverde.backend.fachadaServices.services.compraService;

import java.time.LocalDate;
import java.util.List;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCompra;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCompraRequest;
import com.casaverde.backend.fachadaServices.dto.CompraDTO;
import com.casaverde.backend.fachadaServices.dto.CompraRequest;

public interface ICompraService {
    List<CompraDTO> listar(LocalDate fecha, EstadoCompra estado);
    CompraDTO obtener(Long id);
    CompraDTO registrar(CompraRequest req);
    void anular(Long id);
    AbonoCompraDTO registrarAbono(Long compraId, AbonoCompraRequest req);
    List<AbonoCompraDTO> listarAbonos(Long compraId);
}
