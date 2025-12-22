package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createContactDetails;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.ContactDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ContactDetailsToContactDetailsEntityTest extends ConverterBaseTest{

    @InjectMocks
    private ContactDetailsToContactDetailsEntity converter;

    @Test
    public void testConvertDefendantToDefendantDetails() {
        final ContactDetails contactDetails = converter.convert(createContactDetails());
        assertContactDetails(contactDetails);
    }
}