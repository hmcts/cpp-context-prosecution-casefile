package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.JurisdictionType;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.JurisdictionType.valueFor;

public class JurisdictionTypeMapper {

    public static final Function<JurisdictionType, uk.gov.justice.core.courts.JurisdictionType> convertJurisdictionType
            = sourceJurisdictionType -> ofNullable(sourceJurisdictionType)
            .map(jurisdictionType -> valueFor(jurisdictionType.name()))
            .map(Optional::get)
            .orElse(null);

    private JurisdictionTypeMapper() {
    }
}
