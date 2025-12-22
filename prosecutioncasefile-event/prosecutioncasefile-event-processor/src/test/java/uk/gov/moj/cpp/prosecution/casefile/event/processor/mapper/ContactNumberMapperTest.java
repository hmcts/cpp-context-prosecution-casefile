package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.ContactNumber;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;

import org.junit.jupiter.api.Test;

public class ContactNumberMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicantOrganisationPersonContactNumber() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final ContactNumber sourceContactNumber = sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getContact();

        final uk.gov.justice.core.courts.ContactNumber targetContactNumber = ContactMapper.convertContact.apply(sourceContactNumber);
        assertContactNumber(sourceContactNumber, targetContactNumber);
    }

    @Test
    public void shouldConvertContactReturnNullWhenSourceContactNumberIsNull() {
        final uk.gov.justice.core.courts.ContactNumber targetContactNumber = ContactMapper.convertContact.apply(null);
        assertTrue(isNull(targetContactNumber));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsPersonDetailsContactNumber() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final ContactNumber sourceContactNumber = sourceCourtApplication.getRespondents().get(0).getPersonDetails().getContact();

        final uk.gov.justice.core.courts.ContactNumber targetContactNumber = ContactMapper.convertContact.apply(sourceContactNumber);
        assertContactNumber(sourceContactNumber, targetContactNumber);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsOrganisationPersonsPersonContactNumber() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final ContactNumber sourceContactNumber = sourceCourtApplication.getRespondents().get(0).getOrganisationPersons().get(0).getPerson().getContact();

        final uk.gov.justice.core.courts.ContactNumber targetContactNumber = ContactMapper.convertContact.apply(sourceContactNumber);
        assertContactNumber(sourceContactNumber, targetContactNumber);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsOrganisationContactNumber() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final ContactNumber sourceContactNumber = sourceCourtApplication.getRespondents().get(0).getOrganisation().getContact();

        final uk.gov.justice.core.courts.ContactNumber targetContactNumber = ContactMapper.convertContact.apply(sourceContactNumber);
        assertContactNumber(sourceContactNumber, targetContactNumber);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPariesPersonDetailsContactNumber() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final ContactNumber sourceContactNumber = sourceCourtApplication.getThirdParties().get(0).getPersonDetails().getContact();

        final uk.gov.justice.core.courts.ContactNumber targetContactNumber = ContactMapper.convertContact.apply(sourceContactNumber);
        assertContactNumber(sourceContactNumber, targetContactNumber);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPariesOrganisationPersonsPersonContactNumber() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final ContactNumber sourceContactNumber = sourceCourtApplication.getThirdParties().get(0).getOrganisationPersons().get(0).getPerson().getContact();

        final uk.gov.justice.core.courts.ContactNumber targetContactNumber = ContactMapper.convertContact.apply(sourceContactNumber);
        assertContactNumber(sourceContactNumber, targetContactNumber);
    }

    private void assertContactNumber(final ContactNumber sourceContactNumber, final uk.gov.justice.core.courts.ContactNumber targetContactNumber) {
        assertThat(targetContactNumber.getWork(), is(sourceContactNumber.getWork()));
        assertThat(targetContactNumber.getHome(), is(sourceContactNumber.getHome()));
        assertThat(targetContactNumber.getMobile(), is(sourceContactNumber.getMobile()));
        assertThat(targetContactNumber.getPrimaryEmail(), is(sourceContactNumber.getPrimaryEmail()));
        assertThat(targetContactNumber.getSecondaryEmail(), is(sourceContactNumber.getSecondaryEmail()));
        assertThat(targetContactNumber.getFax(), is(sourceContactNumber.getFax()));
    }
}