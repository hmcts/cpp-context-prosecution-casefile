package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.address;

import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileToCCLegalEntityDefendantConverterTest {

    @InjectMocks
    private ProsecutionCaseFileToCCLegalEntityDefendantConverter prosecutionCaseFileToCCLegalEntityDefendantConverter;

    @Test
    public void convertTitleWhenTitleIsWithinExpectedList() {
        final Defendant defendant = buildProsecutionCaseDefendant();

        LegalEntityDefendant legalEntityDefendant = prosecutionCaseFileToCCLegalEntityDefendantConverter.convert(defendant);

        assertThat(legalEntityDefendant, is(notNullValue()));
        assertThat(legalEntityDefendant.getOrganisation().getName(), is("OrganisationName"));
        assertThat(legalEntityDefendant.getOrganisation().getContact().getPrimaryEmail(), is("emailaddress"));
        assertThat(legalEntityDefendant.getOrganisation().getContact().getSecondaryEmail(), is("emailaddress2"));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress1(), is("address1"));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress2(), is("address2"));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress3(), is("address3"));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress4(), is("address4"));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getAddress5(), is("address5"));
        assertThat(legalEntityDefendant.getOrganisation().getAddress().getPostcode(), is("postcode"));
    }


    public static Defendant buildProsecutionCaseDefendant() {
        return Defendant.defendant()
                .withId(randomUUID().toString())
                .withOrganisationName("OrganisationName")
                .withEmailAddress1("emailaddress")
                .withEmailAddress2("emailaddress2")
                .withAddress(address()
                        .withAddress1("address1")
                        .withAddress2("address2")
                        .withAddress3("address3")
                        .withAddress4("address4")
                        .withAddress5("address5")
                        .withPostcode("postcode")
                        .build())
                .build();
    }


}