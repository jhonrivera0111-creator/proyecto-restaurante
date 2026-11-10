package com.casaverde.backend.fachadaServices.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparativoMensualDTO {

    private Integer año;
    private List<FilaMes> meses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaMes {
        private Integer mes;        // 1–12
        private BigDecimal gallina; // unidades consumidas ese mes (ventas finalizadas)
        private BigDecimal cuy;
    }
}
