package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DATE_OF_HEARING_MORE_THAN_31DAYS_IN_PAST;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateOfHearingMoreThan31DaysInPastValidationAndEnricherRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final int MAX_DAYS_IN_PAST = 31;

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if (defendantWithReferenceData.isMCCWithListNewHearing() || defendantWithReferenceData.isInactiveMigratedCase()) {
            return VALID;
        }
        final String dateOfHearing = defendantWithReferenceData.getDefendant().getInitialHearing().getDateOfHearing();
        final LocalDate hearingDate = convertToLocalDate(dateOfHearing);

        if (hearingDate.isBefore(LocalDate.now().minusDays(MAX_DAYS_IN_PAST))) {
            return newValidationResult(of(newProblem(DATE_OF_HEARING_MORE_THAN_31DAYS_IN_PAST, new ProblemValue(null, FieldName.DEFENDANT_DATE_OF_HEARING.getValue(), dateOfHearing))));
        }
        return VALID;
    }

    private LocalDate convertToLocalDate(final String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

}
