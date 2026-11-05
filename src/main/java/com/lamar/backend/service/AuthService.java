package com.lamar.backend.service;

import com.lamar.backend.dto.LoginRequest;
import com.lamar.backend.dto.LoginResponse;
import com.lamar.backend.repository.EmpleadoRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.expiration-minutes:480}")
    private long expirationMinutes;

    public LoginResponse login(LoginRequest request) {
        // Mismo mensaje para usuario inexistente y contraseña incorrecta (no revelar
        // cuál falló)
        var empleado = empleadoRepository.findByDni(request.usuario())
                .filter(e -> e.getPasswordHash() != null
                        && passwordEncoder.matches(request.password(), e.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"));

        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(empleado.getId()))
                .issuedAt(now)
                .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
                .claim("nombre", empleado.getNombres() + " " + empleado.getApellidos())
                .claim("cargo", empleado.getCargo())
                .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new LoginResponse(token, expirationMinutes, empleado.getId(),
                empleado.getNombres(), empleado.getApellidos(), empleado.getCargo());
    }
}