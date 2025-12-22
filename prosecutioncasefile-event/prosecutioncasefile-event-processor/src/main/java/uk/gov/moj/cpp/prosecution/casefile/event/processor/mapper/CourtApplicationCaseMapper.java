package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplicationCase;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CourtApplicationCaseMapper {

    public static final Function<List<CourtApplicationCase>, List<uk.gov.justice.core.courts.CourtApplicationCase>> convertCourtApplicationCase
            = sourceCourtApplicationCases -> ofNullable(sourceCourtApplicationCases)
            .map(courtApplicationCases -> courtApplicationCases.stream()
                    .map(sourceCourtApplicationCase -> courtApplicationCase()
                            .withProsecutionCaseIdentifier(sourceCourtApplicationCase.getProsecutionCaseIdentifier())
                            .withCaseStatus(sourceCourtApplicationCase.getCaseStatus())
                            .withIsSJP(sourceCourtApplicationCase.getIsSJP())
                            .withProsecutionCaseId(sourceCourtApplicationCase.getProsecutionCaseId())
                            .withOffences(sourceCourtApplicationCase.getOffences())
                            .build())
                    .collect(Collectors.toList())
            )
            .orElse(null);

    private CourtApplicationCaseMapper() {
    }
}