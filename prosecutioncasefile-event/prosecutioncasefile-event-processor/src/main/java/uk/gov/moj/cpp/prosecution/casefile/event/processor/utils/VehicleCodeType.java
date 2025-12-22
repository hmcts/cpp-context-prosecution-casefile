package uk.gov.moj.cpp.prosecution.casefile.event.processor.utils;

import static java.util.Optional.of;

import uk.gov.justice.core.courts.VehicleCode;

import java.util.Optional;

public enum VehicleCodeType {
    OTHER("O"),

    LARGE_GOODS_VEHICLE("L");

    private final String cjsCode;

    VehicleCodeType(String cjsCode) {
        this.cjsCode = cjsCode;
    }

    public static Optional<VehicleCode> valueFor(final String cjsCode) {
        if (OTHER.getCjsCode().equals(cjsCode)) {
            return of(VehicleCode.OTHER);
        } else if (LARGE_GOODS_VEHICLE.getCjsCode().equals(cjsCode)) {
            return of(VehicleCode.LARGE_GOODS_VEHICLE);
        }
        return Optional.empty();
    }

    public String getCjsCode() {
        return cjsCode;
    }
}
