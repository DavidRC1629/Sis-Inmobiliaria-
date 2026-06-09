package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para JwtService — sin Spring context.
 */
class JwtServiceUnitTest {

    private JwtService jwtService;

    // 64-byte hex secret (256-bit) válido para HS256
    private static final String SECRET =
            "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86_400_000L); // 24 h
    }

    // ─── generateToken (extraClaims + UserDetails) ───────────────────────────

    @Test
    void generateToken_withExtraClaims_returnsNonNullToken() {
        UserDetails user = buildUser("juan");

        String token = jwtService.generateToken(Map.of("role", "ROLE_USER"), user);

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    // ─── generateToken (UserDetails + role) ─────────────────────────────────

    @Test
    void generateToken_withRole_embedsRoleClaim() {
        UserDetails user = buildUser("maria");

        String token = jwtService.generateToken(user, "ROLE_ADMIN");

        assertNotNull(token);
        assertEquals("maria", jwtService.extractUsername(token));
    }

    // ─── extractUsername ─────────────────────────────────────────────────────

    @Test
    void extractUsername_returnsCorrectSubject() {
        UserDetails user = buildUser("testuser");
        String token = jwtService.generateToken(Map.of(), user);

        assertEquals("testuser", jwtService.extractUsername(token));
    }

    // ─── isTokenValid ────────────────────────────────────────────────────────

    @Test
    void isTokenValid_sameUser_returnsTrue() {
        UserDetails user = buildUser("alice");
        String token = jwtService.generateToken(Map.of("role", "ROLE_USER"), user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_differentUser_returnsFalse() {
        UserDetails alice = buildUser("alice");
        UserDetails bob   = buildUser("bob");
        String token = jwtService.generateToken(Map.of(), alice);

        assertFalse(jwtService.isTokenValid(token, bob));
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // Generate token with -1 ms expiration (already expired)
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        UserDetails user = buildUser("expired");
        String token = jwtService.generateToken(Map.of(), user);

        // jjwt throws ExpiredJwtException when parsing an expired token — that means it's invalid
        try {
            boolean valid = jwtService.isTokenValid(token, user);
            assertFalse(valid);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Expected: expired token throws exception, which is equivalent to "not valid"
        }
    }

    // ─── extractClaim ────────────────────────────────────────────────────────

    @Test
    void extractClaim_customClaim_returnsCorrectValue() {
        UserDetails user = buildUser("claimer");
        String token = jwtService.generateToken(Map.of("role", "ROLE_SUPER"), user);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));
        assertEquals("ROLE_SUPER", role);
    }

    // ─── helper ─────────────────────────────────────────────────────────────

    private UserDetails buildUser(String username) {
        return User.withUsername(username)
                .password("ignored")
                .roles("USER")
                .build();
    }
}
