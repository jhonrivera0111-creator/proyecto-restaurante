package com.casaverde.backend.fachadaServices.services.cajaService;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.casaverde.backend.fachadaServices.dto.AjusteCapitalRequest;
import com.casaverde.backend.fachadaServices.dto.EntradaFondoRequest;
import com.casaverde.backend.fachadaServices.dto.EstadoNequiMesDTO;
import com.casaverde.backend.fachadaServices.dto.FondoDTO;
import com.casaverde.backend.fachadaServices.dto.MovimientoFondoDTO;
import com.casaverde.backend.fachadaServices.dto.TransferenciaFondoRequest;

public interface IFondoService {

    List<FondoDTO> listar();

    FondoDTO ajustar(String moneda, AjusteCapitalRequest req);

    FondoDTO registrarEntrada(EntradaFondoRequest req);

    void registrarTransferencia(TransferenciaFondoRequest req);

    FondoDTO crearCuentaNequi(String alias);

    void eliminarCuentaNequi(String moneda);

    String getCuentaNequiActiva();

    EstadoNequiMesDTO estadoNequiMes(String fondoMoneda);

    List<MovimientoFondoDTO> historial();

    List<MovimientoFondoDTO> historial(LocalDate fecha, String moneda, String tipo);

    Page<MovimientoFondoDTO> historialPaginado(LocalDate fecha, String moneda, String tipo, Pageable pageable);
}
