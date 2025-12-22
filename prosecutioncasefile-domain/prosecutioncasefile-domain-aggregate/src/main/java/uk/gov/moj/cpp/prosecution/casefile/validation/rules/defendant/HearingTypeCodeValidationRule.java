package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Objects;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.HEARING_TYPE_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.POLICE_FORCE_CODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.HEARING_TYPE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.POLICE_FORCE_CODE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;


public class HearingTypeCodeValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {

        final InitialHearing initialHearing = defendantWithReferenceData.getDefendant().getInitialHearing();

        if (Objects.isNull(initialHearing)) {
            return VALID;
        }

        if (Objects.nonNull(initialHearing.getHearingTypeCode())) {
            final String hearingTypeCode = initialHearing.getHearingTypeCode();

            if (referenceDataQueryService.retrieveHearingTypes().getHearingtypes().stream().anyMatch(x -> x.getHearingCode().equals(hearingTypeCode))) {
                return VALID;
            } else {
                return newValidationResult(of(newProblem(HEARING_TYPE_CODE_INVALID, new ProblemValue(null, HEARING_TYPE_CODE.getValue(), hearingTypeCode))));
            }
        } else {
            return VALID;
        }
    }
}