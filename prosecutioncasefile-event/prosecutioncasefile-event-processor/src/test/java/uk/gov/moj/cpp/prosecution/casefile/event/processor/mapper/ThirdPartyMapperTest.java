package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ThirdParty;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ThirdPartyMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdParties() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();

        final Person sourcePerson = PersonMapper.convertPerson.apply(sourceCourtApplication.getThirdParties().get(0).getPersonDetails());
        final Organisation sourceOrganisation = OrganisationMapper.convertApplicantOrganisation.apply(sourceCourtApplication.getThirdParties().get(0).getOrganisation());
        final List<AssociatedPerson> sourceAssociatedPersonList = OrganisationPersonMapper.convertAssociatedPerson.apply(sourceCourtApplication.getThirdParties().get(0).getOrganisationPersons());

        final List<CourtApplicationParty> targetCourtApplicationParty = ThirdPartyMapper.convertThirdParty.apply(sourceCourtApplication.getThirdParties());
        assertThat(targetCourtApplicationParty.size(), is(sourceCourtApplication.getThirdParties().size()));
        assertThat(targetCourtApplicationParty.get(0).getSummonsRequired(), is(sourceCourtApplication.getThirdParties().get(0).getSummonsRequired()));
        assertThat(targetCourtApplicationParty.get(0).getNotificationRequired(), is(sourceCourtApplication.getThirdParties().get(0).getNotificationRequired()));
        assertThat(targetCourtApplicationParty.get(0).getAppointmentNotificationRequired(), is(sourceCourtApplication.getThirdParties().get(0).getAppointmentNotificationRequired()));
        assertThat(targetCourtApplicationParty.get(0).getPersonDetails(), is(sourcePerson));
        assertThat(targetCourtApplicationParty.get(0).getOrganisation(), is(sourceOrganisation));
        assertThat(targetCourtApplicationParty.get(0).getOrganisationPersons(), is(sourceAssociatedPersonList));
        assertTrue(nonNull(targetCourtApplicationParty.get(0).getId()));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationWhenThirdPartiesIsEmptyOrNull() {
        final List<ThirdParty> sourceThirdParties = emptyList();

        final List<CourtApplicationParty> targetCourtApplicationParty =
                ThirdPartyMapper.convertThirdParty.apply(sourceThirdParties);
        assertThat(targetCourtApplicationParty.size(), is(0));

        final List<CourtApplicationParty> targetCourtApplicationPartyNull =
                ThirdPartyMapper.convertThirdParty.apply(null);
        assertThat(targetCourtApplicationPartyNull, nullValue());
    }
}