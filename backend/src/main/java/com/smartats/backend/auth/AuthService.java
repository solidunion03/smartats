package com.smartats.backend.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(RegisterRequest request) {
        // refuser si l'email existe déjà
        if (appUserRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyUsedException(request.email());
        }

        // hacher le mot de passe (jamais stocké en clair)
        String hash = passwordEncoder.encode(request.password());

        AppUser user = new AppUser(
                UUID.randomUUID(),
                request.email(),
                hash,
                "CLIENT",                 // rôle par défaut à l'inscription
                OffsetDateTime.now()
        );
        appUserRepository.save(user);

        return new RegisterResponse(user.getEmail(), user.getRole());
    }
    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        // re-hacher le mot de passe fourni et comparer au hash stocké
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // identité vérifiée → fabriquer le jeton
        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new LoginResponse(token);
    }
}