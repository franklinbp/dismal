package com.dismal.distribuciones.modules.security.util;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.dismal.distribuciones.exception.BadRequestException;

public final class PhoneNormalizer {

    private static final String DEFAULT_REGION = "EC";
    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();

    private PhoneNormalizer() {}

    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("00")) {
            trimmed = "+" + trimmed.substring(2);
        }
        try {
            Phonenumber.PhoneNumber number = PHONE_UTIL.parse(trimmed, DEFAULT_REGION);
            if (!PHONE_UTIL.isValidNumber(number)) {
                throw new BadRequestException("Telefono invalido.");
            }
            return PHONE_UTIL.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            throw new BadRequestException("Telefono invalido.");
        }
    }
}
