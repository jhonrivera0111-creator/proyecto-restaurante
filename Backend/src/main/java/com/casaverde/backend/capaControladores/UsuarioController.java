package com.casaverde.backend.capaControladores;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.casaverde.backend.fachadaServices.dto.CambiarPasswordRequest;
import com.casaverde.backend.fachadaServices.dto.CrearOperadorRequest;
import com.casaverde.backend.fachadaServices.dto.LoginRequest;
import com.casaverde.backend.fachadaServices.dto.UsuarioDTO;
import com.casaverde.backend.fachadaServices.services.productoService.IUsuarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UsuarioController {

    private final IUsuarioService service;

    @GetMapping("/api/v1/usuarios")
    public List<UsuarioDTO> listar(@RequestParam(defaultValue = "false") boolean todos) {
        return todos ? service.listarTodos() : service.listarActivos();
    }

    @PostMapping("/api/v1/sesion/login")
    public UsuarioDTO login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/api/v1/usuarios")
    public UsuarioDTO crearOperador(@Valid @RequestBody CrearOperadorRequest request) {
        return service.crearOperador(request);
    }

    @PutMapping("/api/v1/usuarios/{id}/toggle")
    public UsuarioDTO toggle(@PathVariable Long id) {
        return service.toggleActivo(id);
    }

    @PutMapping("/api/v1/usuarios/{id}/password")
    public UsuarioDTO cambiarPassword(@PathVariable Long id,
            @Valid @RequestBody CambiarPasswordRequest request) {
        return service.cambiarPassword(id, request);
    }

    @DeleteMapping("/api/v1/usuarios/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminarOperador(id);
    }
}
