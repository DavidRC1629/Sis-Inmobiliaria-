package com.sisarovi.inmobiliario.unit;

import com.sisarovi.inmobiliario.entity.AppSetting;
import com.sisarovi.inmobiliario.repository.AppSettingRepository;
import com.sisarovi.inmobiliario.service.AppSettingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppSettingServiceUnitTest {

    @Mock AppSettingRepository appSettingRepository;
    @InjectMocks AppSettingService appSettingService;

    // ─── getLogoAroviUrl ─────────────────────────────────────────────────

    @Test
    void getLogoAroviUrl_exists_returnsValue() {
        AppSetting setting = AppSetting.builder().key("logo.arovi").value("http://logo.png").build();
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.of(setting));

        String result = appSettingService.getLogoAroviUrl();

        assertEquals("http://logo.png", result);
    }

    @Test
    void getLogoAroviUrl_notExists_returnsEmpty() {
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.empty());

        String result = appSettingService.getLogoAroviUrl();

        assertEquals("", result);
    }

    // ─── saveLogoAroviUrl ────────────────────────────────────────────────

    @Test
    void saveLogoAroviUrl_existing_updatesValue() {
        AppSetting existing = AppSetting.builder().key("logo.arovi").value("old.png").build();
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.of(existing));
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = appSettingService.saveLogoAroviUrl("new.png");

        assertEquals("new.png", result);
        verify(appSettingRepository).save(argThat(s -> "new.png".equals(s.getValue())));
    }

    @Test
    void saveLogoAroviUrl_notExisting_createsNew() {
        when(appSettingRepository.findById("logo.arovi")).thenReturn(Optional.empty());
        when(appSettingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = appSettingService.saveLogoAroviUrl("http://new-logo.png");

        assertEquals("http://new-logo.png", result);
        verify(appSettingRepository).save(argThat(s ->
                "logo.arovi".equals(s.getKey()) && "http://new-logo.png".equals(s.getValue())));
    }
}
