package com.lamar.backend.dto;

import com.lamar.backend.entity.Cliente;

public record ClienteDto(Integer id, String dni, String nombres, String apellidos,
                         String telefono, String correo) {

    public static ClienteDto from(Cliente c) {
        return new ClienteDto(c.getId(), c.getDni(), c.getNombres(), c.getApellidos(),
                c.getTelefono(), c.getCorreo());
    }
}