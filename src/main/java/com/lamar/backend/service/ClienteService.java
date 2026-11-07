package com.lamar.backend.service;

import com.lamar.backend.dto.ClienteDto;
import com.lamar.backend.dto.ClienteRequests;
import com.lamar.backend.entity.Cliente;
import com.lamar.backend.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional(readOnly = true)
    public ClienteDto buscarPorDni(String dni) {
        return clienteRepository.findByDni(dni.trim())
                .map(ClienteDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no registrado"));
    }

    @Transactional
    public ClienteDto crear(ClienteRequests.Crear req) {
        String dni = req.dni().trim();
        if (clienteRepository.existsByDni(dni)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese documento");
        }
        Cliente c = new Cliente();
        c.setDni(dni);
        c.setNombres(req.nombres().trim());
        c.setApellidos(req.apellidos().trim());
        c.setTelefono(vacioANull(req.telefono()));
        c.setCorreo(vacioANull(req.correo()));
        return ClienteDto.from(clienteRepository.save(c));
    }

    @Transactional
    public ClienteDto actualizar(Integer id, ClienteRequests.Actualizar req) {
        Cliente c = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
        c.setNombres(req.nombres().trim());
        c.setApellidos(req.apellidos().trim());
        c.setTelefono(vacioANull(req.telefono()));
        c.setCorreo(vacioANull(req.correo()));
        return ClienteDto.from(c);
    }

    private String vacioANull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}