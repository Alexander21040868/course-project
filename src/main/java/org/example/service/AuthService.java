package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepo, PasswordEncoder encoder, JwtService jwtService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(AuthRequest req) {
        if (userRepo.existsByUsername(req.username())) {
            throw new IllegalArgumentException("Имя пользователя уже занято");
        }
        if (req.email() != null && userRepo.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email уже используется");
        }

        Role role = parseRole(req.role());
        User user = new User(
                req.username(),
                req.email() != null ? req.email() : req.username() + "@codequest.local",
                encoder.encode(req.password()),
                role
        );
        userRepo.save(user);
        log.info("New user registered: {}", user.getUsername());

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(AuthRequest req) {
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> {
                    log.warn("Failed login: user '{}' not found", req.username());
                    return new IllegalArgumentException("Неверный логин или пароль");
                });

        if (!encoder.matches(req.password(), user.getPassword())) {
            log.warn("Failed login: wrong password for '{}'", req.username());
            throw new IllegalArgumentException("Неверный логин или пароль");
        }

        log.info("User '{}' logged in", user.getUsername());
        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    private Role parseRole(String raw) {
        if (raw == null || raw.isBlank()) return Role.STUDENT;
        String normalized = raw.trim().toUpperCase();
        if ("TEACHER".equals(normalized)) return Role.TEACHER;
        if ("STUDENT".equals(normalized)) return Role.STUDENT;
        throw new IllegalArgumentException("Недопустимая роль");
    }
}
