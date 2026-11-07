package com.lamar.backend.dto;

import com.lamar.backend.entity.Proveedor;

public record ProveedorDto(Integer id, String ruc, String razonSocial, String telefono) {

    public static ProveedorDto from(Proveedor p) {
        return new ProveedorDto(p.getId(), p.getRuc(), p.getRazonSocial(), p.getTelefono());
    }
}