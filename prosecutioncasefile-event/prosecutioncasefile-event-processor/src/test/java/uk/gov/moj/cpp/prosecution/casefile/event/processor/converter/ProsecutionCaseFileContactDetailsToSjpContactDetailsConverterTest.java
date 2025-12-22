package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.ContactDetails.contactDetails;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;

import org.junit.jupiter.api.Test;

public class ProsecutionCaseFileContactDetailsToSjpContactDetailsConverterTest {

    private final ProsecutionCaseFileContactDetailsToSjpContactDetailsConverter underTest = new ProsecutionCaseFileContactDetailsToSjpContactDetailsConverter();

    @Test
    public void shouldConverContactDetails() {

        final ContactDetails caseFileContactDetails = ContactDetails.contactDetails()
                .withHome("0207 000 0000")
                .withMobile("11111 111 111")
                .withPrimaryEmail("primary@email.com")
                .withSecondaryEmail("secondary@email.com")
                .withWork("0207 111 1111")
                .build();


        final uk.gov.justice.json.schemas.domains.sjp.ContactDetails expected = contactDetails()
                        .withHome(caseFileContactDetails.getHome())
                        .withMobile(caseFileContactDetails.getMobile())
                        .withBusiness(caseFileContactDetails.getWork())
                        .withEmail(caseFileContactDetails.getPrimaryEmail())
                        .withEmail2(caseFileContactDetails.getSecondaryEmail())
                        .build();

        assertThat(underTest.convert(caseFileContactDetails), equalTo(expected));
    }

}