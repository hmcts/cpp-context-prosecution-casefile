package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createCorporateDefendant;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createDefendant;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.DefendantDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantToDefendantDetailsTest extends ConverterBaseTest {

    @InjectMocks
    private DefendantToDefendantDetails converter;

    @Spy
    private SelfDefinedInformationToSelfDefinedInformationDetails selfDefinedInformationToSelfDefinedInformationDetails;

    @Spy
    @InjectMocks
    private PersonalInformationToPersonalInformationDetails personalInformationToPersonalInformationDetails;

    @Spy
    private OffenceToOffenceDetails offenceToOffenceDetails;

    @Spy
    private AddressToAddressDetails addressToAddressDetails;

    @Spy
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;

    @Test
    public void testConvertDefendantToDefendantDetails() {
        final DefendantDetails defendantDetails = converter.convert(createDefendant());
        assertDefendantDetails(defendantDetails);
    }

    @Test
    public void testConvertDefendantToDefendantDetailsForCorporate() {
         final DefendantDetails defendantDetails = converter.convert(createCorporateDefendant());
         assertCorporateDefendantDetails(defendantDetails);
    }

}