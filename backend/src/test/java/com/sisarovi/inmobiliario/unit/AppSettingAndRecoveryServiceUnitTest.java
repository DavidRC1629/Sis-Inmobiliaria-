package com.sisarovi.inmobiliario.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisarovi.inmobiliario.entity.AppSetting;
import com.sisarovi.inmobiliario.repository.AppSettingRepository;
import com.sisarovi.inmobiliario.service.AppSettingService;
import com.sisarovi.inmobiliario.service.RecoveryEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppSettingAndRecoveryServiceUnitTest {

    // ════════════════════════════════════════════════════════════════
    // AppSettingService
    // ════════════════════════════════════════════════════════════════

    @Mock AppSettingRepository appSettingRepository;
    @InjectMocks AppSettingService appSettingService;

    // ─── getLogoAroviUrl ─────────────────────────────────────────────

    @Test
    void getLogoAroviUrl_existingKey_returnsValue() {
        AppSetting setting = AppSetting.builder().key("logo.arovi").value("http://logo.png").build();
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.of(setting));

        String result = appSettingService.getLogoAroviUrl();

        assertEquals("http://logo.png", result);
    }

    @Test
    void getLogoAroviUrl_notFound_returnsEmpty() {
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.empty());

        String result = appSettingService.getLogoAroviUrl();

        assertEquals("", result);
    }

    // ─── saveLogoAroviUrl ─────────────────────────────────────────────

    @Test
    void saveLogoAroviUrl_existingSetting_updatesValue() {
        AppSetting existing = AppSetting.builder().key("logo.arovi").value("old.png").build();
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.of(existing));
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = appSettingService.saveLogoAroviUrl("new.png");

        assertEquals("new.png", result);
        verify(appSettingRepository).save(argThat(s -> "new.png".equals(s.getValue())));
    }

    @Test
    void saveLogoAroviUrl_noExistingSetting_createsNew() {
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.empty());
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = appSettingService.saveLogoAroviUrl("http://new-logo.png");

        assertEquals("http://new-logo.png", result);
        verify(appSettingRepository).save(any());
    }

    // ════════════════════════════════════════════════════════════════
    // RecoveryEmailService — sin llamadas HTTP reales
    // ════════════════════════════════════════════════════════════════

    private RecoveryEmailService recoveryEmailService;

    @BeforeEach
    void setUpRecovery() {
        recoveryEmailService = new RecoveryEmailService(new ObjectMapper());
    }

    @Test
    void sendTemporaryCode_resendDisabled_doesNothing() {
        ReflectionTestUtils.setField(recoveryEmailService, "resendEnabled", false);
        ReflectionTestUtils.setField(recoveryEmailService, "resendApiKey", "any-key");
        ReflectionTestUtils.setField(recoveryEmailService, "resendFromEmail", "test@test.com");

        // No debe lanzar excepción cuando está deshabilitado
        assertDoesNotThrow(() -> recoveryEmailService.sendTemporaryCode("dest@test.com", "ABCD1234"));
    }

    @Test
    void sendTemporaryCode_noApiKey_throws() {
        ReflectionTestUtils.setField(recoveryEmailService, "resendEnabled", true);
        ReflectionTestUtils.setField(recoveryEmailService, "resendApiKey", "");
        ReflectionTestUtils.setField(recoveryEmailService, "resendFromEmail", "test@test.com");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> recoveryEmailService.sendTemporaryCode("dest@test.com", "ABCD1234"));
        assertTrue(ex.getMessage().contains("API Key"));
    }

    @Test
    void sendTemporaryCode_nullApiKey_throws() {
        ReflectionTestUtils.setField(recoveryEmailService, "resendEnabled", true);
        ReflectionTestUtils.setField(recoveryEmailService, "resendApiKey", null);
        ReflectionTestUtils.setField(recoveryEmailService, "resendFromEmail", "test@test.com");

        assertThrows(RuntimeException.class,
                () -> recoveryEmailService.sendTemporaryCode("dest@test.com", "ABCD1234"));
    }
}
