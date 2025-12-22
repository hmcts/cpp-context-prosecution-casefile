package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Respondent;

import java.util.List;

import org.junit.jupiter.api.Test;

public class RespondentMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondents() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final List<Respondent> sourceCourtApplicationRespondents = sourceCourtApplication.getRespondents();

        final Person sourcePerson = PersonMapper.convertPerson.apply(sourceCourtApplication.getThirdParties().get(0).getPersonDetails());
        final Organisation sourceRepresentationOrganisation = OrganisationMapper.convertApplicantOrganisation.apply(sourceCourtApplicationRespondents.get(0).getRepresentedOrganisation());
        final Organisation sourceOrganisation = OrganisationMapper.convertApplicantOrganisation.apply(sourceCourtApplicationRespondents.get(0).getOrganisation());
        final List<AssociatedPerson> sourceAssociatedPersonList = OrganisationPersonMapper.convertAssociatedPerson.apply(sourceCourtApplicationRespondents.get(0).getOrganisationPersons());

        final List<CourtApplicationParty> targetCourtApplicationParties = RespondentMapper.convertRespondent.apply(sourceCourtApplicationRespondents);
        assertThat(targetCourtApplicationParties.size(), is(sourceCourtApplicationRespondents.size()));
        assertThat(targetCourtApplicationParties.get(0).getPersonDetails(), is(sourcePerson));
        assertThat(targetCourtApplicationParties.get(0).getOrganisation(), is(sourceOrganisation));
        assertThat(targetCourtApplicationParties.get(0).getOrganisationPersons(), is(sourceAssociatedPersonList));
        assertThat(targetCourtApplicationParties.get(0).getRepresentationOrganisation(), is(sourceRepresentationOrganisation));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationWhenRespondentsIsEmptyOrNull() {
        final List<Respondent> sourceCourtApplicationRespondents = emptyList();

        final List<CourtApplicationParty> targetCourtApplicationParties =
                RespondentMapper.convertRespondent.apply(sourceCourtApplicationRespondents);
        assertThat(targetCourtApplicationParties.size(), is(sourceCourtApplicationRespondents.size()));

        final List<CourtApplicationParty> targetCourtApplicationPartiesNull =
                RespondentMapper.convertRespondent.apply(null);
        assertThat(targetCourtApplicationPartiesNull, nullValue());
    }
}