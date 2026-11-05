package com.lamar.backend.config;

import com.lamar.backend.repository.EmpleadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        var pendientes = empleadoRepository.findAll().stream()
                .filter(e -> e.getPasswordHash() == null)
                .toList();
        pendientes.forEach(e -> e.setPasswordHash(passwordEncoder.encode(e.getDni())));
        empleadoRepository.saveAll(pendientes);
    }
}