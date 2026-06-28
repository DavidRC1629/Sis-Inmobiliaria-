package com.sisarovi.inmobiliario;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
                "jwt.secret=ZmFrZXNlY3JldGtleTEyMzQ1Njc4OTA=",
                "jwt.expiration=3600000"
        }
)
@ActiveProfiles("test")
class AppTest {

    @Test
    void contextLoads() {
        // Test vacío para verificar que el contexto de Spring inicia
    }
}