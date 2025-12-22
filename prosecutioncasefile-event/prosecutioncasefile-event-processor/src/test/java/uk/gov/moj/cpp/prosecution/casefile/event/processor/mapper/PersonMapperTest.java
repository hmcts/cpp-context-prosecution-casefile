package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;


import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Person;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;

import org.junit.jupiter.api.Test;

public class PersonMapperTest extends MapperBase {


    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesPersonDetails() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person sourcePerson = sourceCourtApplication.getThirdParties().get(0).getPersonDetails();

        final Address sourceAddress = AddressMapper.convertAddress.apply(sourcePerson.getAddress());
        final ContactNumber sourceContactNumber = ContactMapper.convertContact.apply(sourcePerson.getContact());

        final Person targetPerson = PersonMapper.convertPerson.apply(sourcePerson);
        assertPerson(sourcePerson, sourceAddress, sourceContactNumber, targetPerson);
    }

    @Test
    public void shouldConvertPersonReturnNullWhenSourcePersonIsNull() {
        final Person targetPerson = PersonMapper.convertPerson.apply(null);
        assertTrue(isNull(targetPerson));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesOrganisationPersonsPerson() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person sourcePerson = sourceCourtApplication.getThirdParties().get(0).getOrganisationPersons().get(0).getPerson();

        final Address sourceAddress = AddressMapper.convertAddress.apply(sourcePerson.getAddress());
        final ContactNumber sourceContactNumber = ContactMapper.convertContact.apply(sourcePerson.getContact());

        final Person targetPerson = PersonMapper.convertPerson.apply(sourcePerson);
        assertPerson(sourcePerson, sourceAddress, sourceContactNumber, targetPerson);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicantOrganisationPersonsPerson() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person sourcePerson = sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson();

        final Address sourceAddress = AddressMapper.convertAddress.apply(sourcePerson.getAddress());
        final ContactNumber sourceContactNumber = ContactMapper.convertContact.apply(sourcePerson.getContact());

        final Person targetPerson = PersonMapper.convertPerson.apply(sourcePerson);
        assertPerson(sourcePerson, sourceAddress, sourceContactNumber, targetPerson);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsOrganisationPersonsPerson() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person sourcePerson = sourceCourtApplication.getRespondents().get(0).getOrganisationPersons().get(0).getPerson();

        final Address sourceAddress = AddressMapper.convertAddress.apply(sourcePerson.getAddress());
        final ContactNumber sourceContactNumber = ContactMapper.convertContact.apply(sourcePerson.getContact());

        final Person targetPerson = PersonMapper.convertPerson.apply(sourcePerson);
        assertPerson(sourcePerson, sourceAddress, sourceContactNumber, targetPerson);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsPersonDetails() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person sourcePerson = sourceCourtApplication.getRespondents().get(0).getPersonDetails();

        final Address sourceAddress = AddressMapper.convertAddress.apply(sourcePerson.getAddress());
        final ContactNumber sourceContactNumber = ContactMapper.convertContact.apply(sourcePerson.getContact());

        final Person targetPerson = PersonMapper.convertPerson.apply(sourcePerson);
        assertPerson(sourcePerson, sourceAddress, sourceContactNumber, targetPerson);
    }

    private void assertPerson(final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Person sourcePerson, final Address sourceAddress, final ContactNumber sourceContactNumber, final Person targetPerson) {
        assertThat(targetPerson.getTitle(), is(sourcePerson.getTitle()));
        assertThat(targetPerson.getFirstName(), is(sourcePerson.getFirstName()));
        assertThat(targetPerson.getMiddleName(), is(sourcePerson.getMiddleName()));
        assertThat(targetPerson.getLastName(), is(sourcePerson.getLastName()));
        assertThat(targetPerson.getDateOfBirth(), is(sourcePerson.getDateOfBirth().toString()));
        assertThat(targetPerson.getGender().toString(), is(sourcePerson.getGender().toString()));
        assertThat(targetPerson.getInterpreterLanguageNeeds(), is(sourcePerson.getInterpreterLanguageNeeds()));
        assertThat(targetPerson.getDocumentationLanguageNeeds().toString(), is(sourcePerson.getDocumentationLanguageNeeds().toString()));
        assertThat(targetPerson.getNationalInsuranceNumber(), is(sourcePerson.getNationalInsuranceNumber()));
        assertThat(targetPerson.getSpecificRequirements(), is(sourcePerson.getSpecificRequirements()));
        assertThat(targetPerson.getAddress(), is(sourceAddress));
        assertThat(targetPerson.getContact(), is(sourceContactNumber));
        assertThat(targetPerson.getHearingLanguageNeeds().toString(), is(sourcePerson.getHearingLanguageNeeds().toString()));
    }
}