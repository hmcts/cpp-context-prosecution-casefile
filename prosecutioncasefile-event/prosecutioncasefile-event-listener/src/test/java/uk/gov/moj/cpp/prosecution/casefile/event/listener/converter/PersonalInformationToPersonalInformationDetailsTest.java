package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createPersonalInformation;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.PersonalInformationDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PersonalInformationToPersonalInformationDetailsTest extends ConverterBaseTest{

    @InjectMocks
    private PersonalInformationToPersonalInformationDetails converter;

    @Spy
    private AddressToAddressDetails addressToAddressDetails;

    @Spy
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;

    @Test
    public void testConvertPersonalInformationToPersonalInformationDetails() {
        final PersonalInformationDetails personalInformationDetails = converter.convert(createPersonalInformation());
        assertPersonalInformationDetails(personalInformationDetails);
    }
}