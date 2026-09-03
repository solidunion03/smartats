package com.smartats.backend.auth;

public record LoginRequest(String email, String password) {
}