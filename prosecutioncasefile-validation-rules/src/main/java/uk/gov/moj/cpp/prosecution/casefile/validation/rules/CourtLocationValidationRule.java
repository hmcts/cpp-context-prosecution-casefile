package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtCentre;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.SubmitApplication;
import uk.gov.moj.cpp.prosecution.casefile.domain.AdditionalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;

import java.util.Optional;

public class CourtLocationValidationRule extends AbstractValidationRule implements SubmitApplicationValidationRule {

    public CourtLocationValidationRule() {
        super(ValidationError.COURT_LOCATION_REQUIRED);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitApplication submitApplication, final AdditionalInformation additionalInformation) {
        final CourtCentre courtCentre = submitApplication.getBoxHearingRequest().getCourtCentre();
        if (nonNull(courtCentre) && nonNull(courtCentre.getName()) && isValidCourtLocation(courtCentre, additionalInformation.getCourtroomReferenceData())) {
            return empty();
        }
        return of(getValidationError());
    }

    private boolean isValidCourtLocation(final CourtCentre courtCentre, final OrganisationUnitWithCourtroomReferenceData courtroomReferenceData) {
        return isCourtCenterNameMatches(courtCentre, courtroomReferenceData) && isCourtCenterCodeMatches(courtCentre, courtroomReferenceData);
    }

    private boolean isCourtCenterNameMatches(final CourtCentre courtCentre, final OrganisationUnitWithCourtroomReferenceData courtroomReferenceData) {
        return nonNull(courtroomReferenceData) && nonNull(courtroomReferenceData.getOucodeL3Name()) && courtroomReferenceData.getOucodeL3Name().equalsIgnoreCase(courtCentre.getName());
    }

    private boolean isCourtCenterCodeMatches(final CourtCentre courtCentre, final OrganisationUnitWithCourtroomReferenceData courtroomReferenceData) {
        return isNull(courtCentre.getCode()) || (nonNull(courtroomReferenceData.getOucode()) && courtroomReferenceData.getOucode().equals(courtCentre.getCode()));
    }
}
