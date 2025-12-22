package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper.OrganisationMapper.convertApplicantOrganisation;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Applicant;

import java.util.function.Function;

public class ApplicantMapper {

    public static final Function<Applicant, CourtApplicationParty> convertApplicant
            = sourceApplicant -> ofNullable(sourceApplicant)
            .map(applicant -> courtApplicationParty()
                    .withId(sourceApplicant.getId())
                    .withOrganisation(convertApplicantOrganisation.apply(sourceApplicant.getOrganisation()))
                    .withOrganisationPersons(OrganisationPersonMapper.convertAssociatedPerson.apply(sourceApplicant.getOrganisationPersons()))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .orElse(null);

    private ApplicantMapper() {
    }
}
