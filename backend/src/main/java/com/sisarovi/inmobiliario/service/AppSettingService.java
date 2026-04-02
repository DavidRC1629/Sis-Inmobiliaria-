package com.sisarovi.inmobiliario.service;

import com.sisarovi.inmobiliario.entity.AppSetting;
import com.sisarovi.inmobiliario.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    private static final String LOGO_AROVI_KEY = "logo.arovi";

    private final AppSettingRepository appSettingRepository;

    @Transactional(readOnly = true)
    public String getLogoAroviUrl() {
        return appSettingRepository.findById(LOGO_AROVI_KEY)
                .map(AppSetting::getValue)
                .orElse("");
    }

    @Transactional
    public String saveLogoAroviUrl(String value) {
        AppSetting setting = appSettingRepository.findById(LOGO_AROVI_KEY)
                .orElse(AppSetting.builder().key(LOGO_AROVI_KEY).build());
        setting.setValue(value);
        return appSettingRepository.save(setting).getValue();
    }
}
