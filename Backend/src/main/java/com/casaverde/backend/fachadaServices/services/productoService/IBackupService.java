package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.BackupDTO;

public interface IBackupService {

    BackupDTO crearBackup(String motivo);

    List<BackupDTO> listar();
}
