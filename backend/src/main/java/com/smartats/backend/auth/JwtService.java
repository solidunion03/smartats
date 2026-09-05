package com.smartats.backend.auth;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${smartats.jwt.secret}") String secret,
                      @Value("${smartats.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(email)              // le "sub" : qui est-ce
                .claim("role", role)         // le rôle, pour les droits
                .issuedAt(now)               // "iat" : fabriqué maintenant
                .expiration(expiry)          // "exp" : expire dans 1h
                .signWith(key)               // le TAMPON : signé avec la clé secrète
                .compact();                  // produit la chaîne finale xxx.yyy.zzz
    }
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);   // si le parsing réussit, signature OK et non expiré
            return true;
        } catch (Exception e) {
            return false;         // signature invalide, expiré, malformé...
        }
    }

    private io.jsonwebtoken.Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)        // vérifie la signature avec NOTRE clé secrète
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}