package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.core.courts.Person;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.AssociatedPerson;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;

import java.util.List;

import org.junit.jupiter.api.Test;

public class OrganisationPersonMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesApplicantOrganisationPersons() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final List<AssociatedPerson> sourceAssociatedPersonList = sourceCourtApplication.getThirdParties().get(0).getOrganisationPersons();

        final Person sourcePerson = PersonMapper.convertPerson.apply(sourceCourtApplication.getThirdParties().get(0).getPersonDetails());

        final List<uk.gov.justice.core.courts.AssociatedPerson> targetAssociatedPerson = OrganisationPersonMapper.convertAssociatedPerson.apply(sourceAssociatedPersonList);
        assertThat(targetAssociatedPerson.get(0).getRole(), is(sourceAssociatedPersonList.get(0).getRole()));
        assertThat(targetAssociatedPerson.get(0).getPerson(), is(sourcePerson));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationWhenThirdPartiesApplicantOrganisationPersonsItIsEmptyOrNull() {

        final List<AssociatedPerson> sourceAssociatedPersonList = emptyList();

        final List<uk.gov.justice.core.courts.AssociatedPerson> targetAssociatedPersonEmpty = OrganisationPersonMapper
                .convertAssociatedPerson.apply(sourceAssociatedPersonList);
        assertThat(targetAssociatedPersonEmpty.size(), is(0));

        final List<uk.gov.justice.core.courts.AssociatedPerson> targetAssociatedPersonNull = OrganisationPersonMapper
                .convertAssociatedPerson.apply(null);
        assertThat(targetAssociatedPersonNull, nullValue());
    }
}