package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.COURT_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

public abstract class CourtValidationRules implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    protected ValidationResult validateResult(final ReferenceDataQueryService referenceDataQueryService, final FieldName fieldName, final String court){
        if (court == null) {
            return VALID;
        }

        if (referenceDataQueryService.retrieveOrganisationUnits(court).isEmpty()) {
            return newValidationResult(of(newProblem(COURT_LOCATION_OUCODE_INVALID, new ProblemValue(null, fieldName.getValue(), court))));
        } else {
            return VALID;
        }
    }
}
