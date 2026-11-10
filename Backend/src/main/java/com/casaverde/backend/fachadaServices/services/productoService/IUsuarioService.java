package com.casaverde.backend.fachadaServices.services.productoService;

import java.util.List;

import com.casaverde.backend.fachadaServices.dto.CambiarPasswordRequest;
import com.casaverde.backend.fachadaServices.dto.CrearOperadorRequest;
import com.casaverde.backend.fachadaServices.dto.LoginRequest;
import com.casaverde.backend.fachadaServices.dto.UsuarioDTO;

public interface IUsuarioService {

    List<UsuarioDTO> listarActivos();

    List<UsuarioDTO> listarTodos();

    UsuarioDTO login(LoginRequest request);

    void ensureUsuariosIniciales();

    String hashPassword(String password);

    UsuarioDTO crearOperador(CrearOperadorRequest request);

    UsuarioDTO toggleActivo(Long id);

    UsuarioDTO cambiarPassword(Long id, CambiarPasswordRequest request);

    void eliminarOperador(Long id);
}
