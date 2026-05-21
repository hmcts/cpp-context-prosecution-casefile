package uk.gov.moj.cpp.prosecution.casefile;

import static java.util.List.of;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantsWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.event.CaseValidationFailed;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ValidationHelper {

    public static final String SOW_REF_VALUE_MOJ = "moj";

    private ValidationHelper() {
    }

    public static CaseValidationFailed buildCaseValidationFailedEvent(final Prosecution prosecution, final UUID externalId, final List<Problem> caseProblems, final DefendantsWithReferenceData defendantsWithReferenceData) {
        final InitialHearing initialHearing = defendantsWithReferenceData.getDefendants().isEmpty() ? null : defendantsWithReferenceData.getDefendants().get(0).getInitialHearing();
        return new CaseValidationFailed(prosecution, caseProblems, externalId, initialHearing);
    }

    public static  List<OffenceReferenceData> offenceReferenceDataList (final ReferenceDataQueryService referenceDataQueryService, final Offence offence, final String initiationCode, final boolean isCivil) {
        List<OffenceReferenceData> newOffenceReferenceDataList;
        if (isCivil) {
            newOffenceReferenceDataList = referenceDataQueryService.retrieveOffenceDataList(of(offence.getOffenceCode()), Optional.of(SOW_REF_VALUE_MOJ)).stream()
                    .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull)
                    .toList();
        } else {
            newOffenceReferenceDataList = referenceDataQueryService.retrieveOffenceData(offence, initiationCode).stream()
                    .filter(rd -> rd.getCjsOffenceCode().equals(offence.getOffenceCode())).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return newOffenceReferenceDataList;
    }
}
