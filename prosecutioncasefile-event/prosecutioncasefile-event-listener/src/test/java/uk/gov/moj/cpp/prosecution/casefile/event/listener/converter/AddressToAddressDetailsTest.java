package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createAddress;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.AddressDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddressToAddressDetailsTest extends ConverterBaseTest {

    @InjectMocks
    private AddressToAddressDetails converter;

    @Test
    public void testConvertDefendantToDefendantDetails() {
        final AddressDetails addressDetails = converter.convert(createAddress());
        assertAddressDetails(addressDetails);
    }
}