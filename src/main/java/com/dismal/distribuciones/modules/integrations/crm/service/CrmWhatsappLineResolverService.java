package com.dismal.distribuciones.modules.integrations.crm.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.dismal.distribuciones.modules.integrations.settings.domain.IntegrationSettings;
import com.dismal.distribuciones.modules.integrations.settings.service.IntegrationSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class CrmWhatsappLineResolverService {

    private static final String DEFAULT_REGION = "EC";
    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private final IntegrationSettingsService settingsService;

    public CrmWhatsappLineResolverService(IntegrationSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Value("${integrations.dismal-crm.whatsapp-id:}")
    private String fallbackWhatsappIdRaw;

    @Value("${integrations.dismal-crm.whatsapp-id-by-country:}")
    private String whatsappIdByCountryRaw;

    public Integer resolveWhatsappId(String phone) {
        IntegrationSettings settings = settingsService.getSettingsEntity();
        String configuredCountryMap = firstNotBlank(settings.getCrmWhatsappIdByCountry(), whatsappIdByCountryRaw);
        String configuredFallbackId = firstNotBlank(settings.getCrmWhatsappDefaultId(), fallbackWhatsappIdRaw);
        Map<String, Integer> countryMap = parseCountryMap(configuredCountryMap);
        String countryCode = resolveCountryCode(phone, countryMap);
        Integer countryWhatsappId = countryCode != null ? countryMap.get(countryCode) : null;
        if (countryWhatsappId != null) {
            return countryWhatsappId;
        }
        return parseWhatsappId(configuredFallbackId);
    }

    public boolean isWhatsappEnabled(boolean fallbackEnabled) {
        IntegrationSettings settings = settingsService.getSettingsEntity();
        if (settings.getCrmWhatsappEnabled() != null) {
            return settings.getCrmWhatsappEnabled();
        }
        return fallbackEnabled;
    }

    private String resolveCountryCode(String phone, Map<String, Integer> configuredCountries) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String digits = phone.replaceAll("\\D", "");
        if (!configuredCountries.isEmpty() && !digits.isBlank()) {
            for (String countryCode : configuredCountries.keySet()) {
                int dialingCode = PHONE_UTIL.getCountryCodeForRegion(countryCode);
                if (dialingCode > 0 && digits.startsWith(String.valueOf(dialingCode))) {
                    return countryCode;
                }
            }
        }

        try {
            Phonenumber.PhoneNumber parsed = PHONE_UTIL.parse(phone, DEFAULT_REGION);
            if (PHONE_UTIL.isValidNumber(parsed)) {
                return PHONE_UTIL.getRegionCodeForNumber(parsed);
            }
        } catch (NumberParseException ignored) {
            log.debug("Could not resolve country for WhatsApp phone {}", phone);
        }
        return null;
    }

    private Map<String, Integer> parseCountryMap(String value) {
        Map<String, Integer> result = new HashMap<>();
        if (value == null || value.isBlank()) {
            return result;
        }

        String[] entries = value.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                log.warn("Invalid DismalCRM country WhatsApp mapping entry: {}", entry);
                continue;
            }

            String countryCode = parts[0].trim().toUpperCase(Locale.ROOT);
            Integer whatsappId = parseWhatsappId(parts[1]);
            if (countryCode.length() == 2 && whatsappId != null) {
                result.put(countryCode, whatsappId);
            } else {
                log.warn("Invalid DismalCRM country WhatsApp mapping entry: {}", entry);
            }
        }
        return result;
    }

    private Integer parseWhatsappId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("Invalid DismalCRM whatsappId configuration: {}", value);
            return null;
        }
    }

    private String firstNotBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }
}
