package uk.gov.moj.cpp.prosecution.casefile.event.processor.mapper;

import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.CourtApplication;

import org.junit.jupiter.api.Test;

public class OrganisationMapperTest extends MapperBase {

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesApplicantOrganisation() {
        final CourtApplication sourceCourtApplication = sourceSubmitApplication.getCourtApplication();
        final uk.gov.moj.cpp.prosecution.casefile.application.json.schemas.Organisation sourceOrganisation = sourceCourtApplication.getThirdParties().get(0).getOrganisation();

        final Address sourceAddress = AddressMapper.convertAddress.apply(sourceOrganisation.getAddress());
        final ContactNumber sourceContactNumber = ContactMapper.convertContact.apply(sourceOrganisation.getContact());

        final Organisation targetOrganisation = OrganisationMapper.convertApplicantOrganisation.apply(sourceOrganisation);
        assertThat(targetOrganisation.getName(), is(sourceOrganisation.getName()));
        assertThat(targetOrganisation.getIncorporationNumber(), is(sourceOrganisation.getIncorporationNumber()));
        assertThat(targetOrganisation.getRegisteredCharityNumber(), is(sourceOrganisation.getRegisteredCharityNumber()));
        assertThat(targetOrganisation.getAddress(), is(sourceAddress));
        assertThat(targetOrganisation.getContact(), is(sourceContactNumber));
    }

    @Test
    public void shouldConvertEventPayloadToCourtApplicationThirdPartiesApplicantOrganisationIsNull() {
        final Organisation targetOrganisation = OrganisationMapper.convertApplicantOrganisation.apply(null);
        assertTrue(isNull(targetOrganisation));
    }
}