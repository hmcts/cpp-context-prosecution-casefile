package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.COURT_HEARING_LOCATION_OUCODE_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_COURT_HEARING_LOCATION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.newValidationResult;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationRule;

import java.util.Optional;

public class CourtHearingLocationValidationRule implements ValidationRule<DefendantWithReferenceData, ReferenceDataQueryService> {

    private static final int COURT_HEARING_OU_CODE_LENGHT = 7;

    @Override
    public ValidationResult validate(final DefendantWithReferenceData defendantWithReferenceData, final ReferenceDataQueryService referenceDataQueryService) {
        if(defendantWithReferenceData.isMCCWithListNewHearing() || defendantWithReferenceData.isInactiveMigratedCase()){
            return VALID;
        }
        final String courtHearingLocation = defendantWithReferenceData.getDefendant().getInitialHearing().getCourtHearingLocation();
        final ReferenceDataVO referenceDataVO = defendantWithReferenceData.getReferenceDataVO();

        if (referenceDataVO.getOrganisationUnitWithCourtroomReferenceData().isPresent()) {
            return VALID;
        }


        if (isValidOuCode(courtHearingLocation)) {
            final Optional<OrganisationUnitWithCourtroomReferenceData> optionalOrganisationUnitWithCourtroomReferenceData = referenceDataQueryService.retrieveOrganisationUnitWithCourtroom(courtHearingLocation);
            if (optionalOrganisationUnitWithCourtroomReferenceData.isPresent()) {
                referenceDataVO.setOrganisationUnitWithCourtroomReferenceData(optionalOrganisationUnitWithCourtroomReferenceData);
                return VALID;
            }
        }

        return newValidationResult(of(newProblem(COURT_HEARING_LOCATION_OUCODE_INVALID, new ProblemValue(null, DEFENDANT_COURT_HEARING_LOCATION.getValue(), courtHearingLocation))));
    }

    private boolean isValidOuCode(final String ouCode) {
        return nonNull(ouCode) && COURT_HEARING_OU_CODE_LENGHT == ouCode.length();
    }
}
