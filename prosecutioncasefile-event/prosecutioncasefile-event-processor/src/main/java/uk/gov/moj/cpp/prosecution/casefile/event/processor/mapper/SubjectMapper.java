package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.MasterDefendantMapper.convertMasterDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.OrganisationMapper.convertApplicantOrganisation;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.OrganisationPersonMapper.convertAssociatedPerson;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.PersonMapper.convertPerson;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Applicant;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;

import java.util.function.Function;

public class SubjectMapper {

    public static final Function<uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication, CourtApplicationParty> assignSubject
            = courtApplication -> {
        if (toBoolean(courtApplication.getApplicant().getIsSubject())) {
            return toCourtApplicationParty(courtApplication.getApplicant());
        } else {
            final Respondent respondent = courtApplication.getRespondents().stream()
                    .filter(Respondent::getIsSubject)
                    .findFirst()
                    .orElse(null);
            if (nonNull(respondent)) {
                return toCourtApplicationParty(respondent);
            }
        }
        return null;
    };

    private SubjectMapper() {
    }

    private static CourtApplicationParty toCourtApplicationParty(final Respondent respondent) {

        return CourtApplicationParty.courtApplicationParty()
                .withRepresentationOrganisation(convertApplicantOrganisation.apply(respondent.getRepresentedOrganisation()))
                .withId(respondent.getId())
                .withPersonDetails(convertPerson.apply(respondent.getPersonDetails()))
                .withOrganisation(convertApplicantOrganisation.apply(respondent.getOrganisation()))
                .withOrganisationPersons(convertAssociatedPerson.apply(respondent.getOrganisationPersons()))
                .withMasterDefendant(convertMasterDefendant.apply(respondent))
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();
    }

    private static CourtApplicationParty toCourtApplicationParty(final Applicant applicant) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(applicant.getId())
                .withOrganisation(convertApplicantOrganisation.apply(applicant.getOrganisation()))
                .withOrganisationPersons(convertAssociatedPerson.apply(applicant.getOrganisationPersons()))
                .withSummonsRequired(false)
                .withNotificationRequired(false)
                .build();
    }

}
