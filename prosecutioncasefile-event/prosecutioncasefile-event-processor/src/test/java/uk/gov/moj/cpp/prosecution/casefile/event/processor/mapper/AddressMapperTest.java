package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.Address;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;

import org.junit.jupiter.api.Test;

public class AddressMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicantOrganisationAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getApplicant().getOrganisation().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicantOrganisationAddressIsNull() {
        final Address targetAddress = AddressMapper.convertAddress.apply(null);
        assertTrue(isNull(targetAddress));
    }


    @Test
    public void shouldConvertEventPayloadToCourtApplicationApplicantOrganisationPersonsPersonAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getApplicant().getOrganisationPersons().get(0).getPerson().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertAddressReturnNullWhenSourceAddressNull() {
        final Address targetAddress = AddressMapper.convertAddress.apply(null);
        assertTrue(isNull(targetAddress));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsPersonDetailsAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getRespondents().get(0).getPersonDetails().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsOrganisationAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getRespondents().get(0).getOrganisation().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsOrganisationPersonsPersonAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getRespondents().get(0).getOrganisationPersons().get(0).getPerson().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationRespondentsRepresentedOrganisationAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getRespondents().get(0).getRepresentedOrganisation().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesPersonDetailsAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getThirdParties().get(0).getPersonDetails().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesOrganisationAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getThirdParties().get(0).getOrganisation().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesOrganisationPersonsPersonAddress() {

        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress = sourceCourtApplication.getThirdParties().get(0).getOrganisationPersons().get(0).getPerson().getAddress();

        final Address targetAddress = AddressMapper.convertAddress.apply(sourceAddress);
        assertAddress(sourceAddress, targetAddress);
    }

    private void assertAddress(final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Address sourceAddress, final Address targetAddress) {
        assertThat(targetAddress.getAddress1(), is(sourceAddress.getAddress1()));
        assertThat(targetAddress.getAddress2(), is(sourceAddress.getAddress2()));
        assertThat(targetAddress.getAddress3(), is(sourceAddress.getAddress3()));
        assertThat(targetAddress.getAddress4(), is(sourceAddress.getAddress4()));
        assertThat(targetAddress.getAddress5(), is(sourceAddress.getAddress5()));
        assertThat(targetAddress.getPostcode(), is(sourceAddress.getPostcode()));
    }
}
