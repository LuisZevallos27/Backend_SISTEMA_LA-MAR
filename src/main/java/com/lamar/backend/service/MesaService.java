package com.lamar.backend.service;

import com.lamar.backend.dto.MesaDto;
import com.lamar.backend.repository.MesaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MesaService {

    private final MesaRepository mesaRepository;

    public List<MesaDto> listar(String zona) {
        var mesas = (zona == null || zona.isBlank())
                ? mesaRepository.findAllByOrderByNumero()
                : mesaRepository.findByZonaOrderByNumero(zona);
        return mesas.stream().map(MesaDto::from).toList();
    }
}