package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ThirdParty;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ThirdPartyMapper {

    public static final Function<List<ThirdParty>, List<CourtApplicationParty>> convertThirdParty
            = sourceThirdParties -> ofNullable(sourceThirdParties)
            .map(thirdParties -> thirdParties.stream()
                    .map(sourceThirdParty -> CourtApplicationParty.courtApplicationParty()
                            .withId(randomUUID())
                            .withSummonsRequired(sourceThirdParty.getSummonsRequired())
                            .withNotificationRequired(sourceThirdParty.getNotificationRequired())
                            .withAppointmentNotificationRequired(sourceThirdParty.getAppointmentNotificationRequired())
                            .withPersonDetails(PersonMapper.convertPerson.apply(sourceThirdParty.getPersonDetails()))
                            .withOrganisation(OrganisationMapper.convertApplicantOrganisation.apply(sourceThirdParty.getOrganisation()))
                            .withOrganisationPersons(OrganisationPersonMapper.convertAssociatedPerson.apply(sourceThirdParty.getOrganisationPersons())).build())
                    .collect(Collectors.toList())
            )
            .orElse(null);

    private ThirdPartyMapper() {
    }
}
