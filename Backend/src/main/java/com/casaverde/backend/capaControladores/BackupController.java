package com.casaverde.backend.capaControladores;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.casaverde.backend.fachadaServices.dto.BackupDTO;
import com.casaverde.backend.fachadaServices.services.productoService.IBackupService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/backups")
@RequiredArgsConstructor
public class BackupController {

    private final IBackupService service;

    @GetMapping
    public List<BackupDTO> listar() {
        return service.listar();
    }

    @PostMapping
    public BackupDTO crear(@RequestParam(defaultValue = "manual") String motivo) {
        return service.crearBackup(motivo);
    }
}
