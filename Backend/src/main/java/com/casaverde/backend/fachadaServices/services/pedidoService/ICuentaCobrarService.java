package com.casaverde.backend.fachadaServices.services.pedidoService;

import java.time.LocalDate;
import java.util.List;

import com.casaverde.backend.capaAccesoADatos.models.enums.EstadoCuentaCobrar;
import com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarDTO;
import com.casaverde.backend.fachadaServices.dto.AbonoCuentaCobrarRequest;
import com.casaverde.backend.fachadaServices.dto.CuentaCobrarDTO;
import com.casaverde.backend.fachadaServices.dto.CuentaCobrarRequest;

public interface ICuentaCobrarService {
    List<CuentaCobrarDTO> listar(EstadoCuentaCobrar estado, LocalDate fecha, String q);
    CuentaCobrarDTO obtener(Long id);
    CuentaCobrarDTO registrar(CuentaCobrarRequest req);
    void anular(Long id);
    AbonoCuentaCobrarDTO registrarAbono(Long cuentaId, AbonoCuentaCobrarRequest req);
    List<AbonoCuentaCobrarDTO> listarAbonos(Long cuentaId);
}
