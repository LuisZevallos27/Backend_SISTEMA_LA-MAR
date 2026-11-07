package com.lamar.backend.service;

import com.lamar.backend.dto.ProveedorDto;
import com.lamar.backend.dto.ProveedorRequests;
import com.lamar.backend.entity.Proveedor;
import com.lamar.backend.repository.ProveedorRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    @Transactional(readOnly = true)
    public List<ProveedorDto> listar() {
        return proveedorRepository.findAllByOrderByRazonSocialAsc().stream().map(ProveedorDto::from).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorDto obtener(Integer id) {
        return ProveedorDto.from(buscar(id));
    }

    @Transactional
    public ProveedorDto crear(ProveedorRequests.Crear req) {
        if (proveedorRepository.existsByRuc(req.ruc())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un proveedor con ese RUC");
        }
        Proveedor p = new Proveedor();
        p.setRuc(req.ruc());
        p.setRazonSocial(req.razonSocial().trim());
        p.setTelefono(vacioANull(req.telefono()));
        return ProveedorDto.from(proveedorRepository.save(p));
    }

    @Transactional
    public ProveedorDto actualizar(Integer id, ProveedorRequests.Actualizar req) {
        Proveedor p = buscar(id);
        p.setRazonSocial(req.razonSocial().trim());
        p.setTelefono(vacioANull(req.telefono()));
        return ProveedorDto.from(p);
    }

    private Proveedor buscar(Integer id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no encontrado"));
    }

    private String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}