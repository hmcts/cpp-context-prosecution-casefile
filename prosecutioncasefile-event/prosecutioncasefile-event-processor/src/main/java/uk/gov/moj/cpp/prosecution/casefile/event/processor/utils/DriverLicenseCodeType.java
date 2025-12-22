package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import uk.gov.justice.core.courts.DriverLicenseCode;

import java.util.Optional;

public enum DriverLicenseCodeType {
    PROVISIONAL("P"),

    FULL("F");

    private final String driverLicenseCode;

    DriverLicenseCodeType(final String driverLicenseCode) {
        this.driverLicenseCode = driverLicenseCode;
    }

    public static Optional<DriverLicenseCode> valueFor(final String driverLicenseCode) {
        for (final DriverLicenseCode type : DriverLicenseCode.values()) {
            if (type.toString().equals(driverLicenseCode)) {
                return DriverLicenseCode.valueFor(type.name());
            }
        }
        return Optional.empty();
    }
}