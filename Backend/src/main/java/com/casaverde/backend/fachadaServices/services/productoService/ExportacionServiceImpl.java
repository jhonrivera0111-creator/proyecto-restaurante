package com.casaverde.backend.fachadaServices.services.productoService;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.casaverde.backend.fachadaServices.services.cajaService.IFondoService;
import com.casaverde.backend.fachadaServices.dto.MovimientoFondoDTO;
import com.casaverde.backend.fachadaServices.dto.PedidoDTO;
import com.casaverde.backend.fachadaServices.dto.ResumenDiaDTO;
import com.casaverde.backend.fachadaServices.services.pedidoService.IResumenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExportacionServiceImpl implements IExportacionService {

    private final IResumenService resumenService;
    private final IFondoService fondoService;

    public String resumenCsv(LocalDate fecha) {
        ResumenDiaDTO resumen = resumenService.obtener(fecha);
        StringBuilder sb = new StringBuilder();
        linea(sb, "fecha", "pedidos_finalizados", "cobrado_total_cop", "gastos_total_cop", "balance_neto");
        linea(sb, resumen.getFecha(), resumen.getPedidosFinalizados(), resumen.getCobradoTotalCop(),
                resumen.getGastosTotalCop(), resumen.getBalanceNeto());
        sb.append(System.lineSeparator());

        linea(sb, "pedido_id", "estado", "total_cop", "abonado_cop", "saldo_cop", "cambio_cop", "finalizado_en");
        for (PedidoDTO pedido : resumen.getPedidos()) {
            linea(sb, pedido.getId(), pedido.getEstado(), pedido.getTotalCop(), pedido.getTotalAbonado(),
                    pedido.getSaldoPendiente(), pedido.getValorCambio(), pedido.getFinalizadoEn());
        }
        return sb.toString();
    }

    public String cajaCsv(LocalDate fecha) {
        List<MovimientoFondoDTO> movimientos = fondoService.historial(fecha, null, null);
        StringBuilder sb = new StringBuilder();
        linea(sb, "fecha", "tipo", "moneda", "monto", "monto_cop", "saldo_resultante", "concepto",
                "referencia_tipo", "referencia_id");
        for (MovimientoFondoDTO movimiento : movimientos) {
            linea(sb, movimiento.getFecha(), movimiento.getTipo(), movimiento.getMoneda(), movimiento.getMonto(),
                    movimiento.getMontoCop(), movimiento.getSaldoResultante(), movimiento.getConcepto(),
                    movimiento.getReferenciaTipo(), movimiento.getReferenciaId());
        }
        return sb.toString();
    }

    private void linea(StringBuilder sb, Object... columnas) {
        for (int i = 0; i < columnas.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(csv(columnas[i]));
        }
        sb.append(System.lineSeparator());
    }

    private String csv(Object valor) {
        if (valor == null) {
            return "";
        }
        String texto = String.valueOf(valor).replace("\"", "\"\"");
        if (texto.contains(";") || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            return "\"" + texto + "\"";
        }
        return texto;
    }
}
