package org.example.service;

import org.example.dto.AuthRequest;
import org.example.dto.AuthResponse;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

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

        User user = new User(
                req.username(),
                req.email() != null ? req.email() : req.username() + "@codequest.local",
                encoder.encode(req.password()),
                Role.STUDENT
        );
        userRepo.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }

    public AuthResponse login(AuthRequest req) {
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new IllegalArgumentException("Неверный логин или пароль"));

        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new IllegalArgumentException("Неверный логин или пароль");
        }

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}
