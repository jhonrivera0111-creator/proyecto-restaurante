package com.casaverde.backend.fachadaServices.services.productoService;

import java.time.LocalDate;

public interface IExportacionService {

    String resumenCsv(LocalDate fecha);

    String cajaCsv(LocalDate fecha);
}
