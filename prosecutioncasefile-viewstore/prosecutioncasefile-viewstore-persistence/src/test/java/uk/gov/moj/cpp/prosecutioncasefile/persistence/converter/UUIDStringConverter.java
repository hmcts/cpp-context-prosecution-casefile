package uk.gov.moj.cpp.prosecutioncasefile.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.UUID;

@Converter(autoApply = true)
public class UUIDStringConverter implements AttributeConverter<UUID, String> {

    @Override
    public String convertToDatabaseColumn(final UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    @Override
    public UUID convertToEntityAttribute(final String s) {
        return s == null ? null : UUID.fromString(s);
    }
}
