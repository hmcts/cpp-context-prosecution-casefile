package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtCentre;

import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;

public class CodeCentreMapper {

    public static final Function<CourtCentre, uk.gov.justice.core.courts.CourtCentre> convertCourtCentre
            = sourceCourtCentre -> ofNullable(sourceCourtCentre)
            .map(scc -> courtCentre()
                    .withId(scc.getId())
                    .withCode(scc.getCode())
                    .withName(scc.getName())
                    .build())
            .orElse(null);

    private CodeCentreMapper() {
    }
}
