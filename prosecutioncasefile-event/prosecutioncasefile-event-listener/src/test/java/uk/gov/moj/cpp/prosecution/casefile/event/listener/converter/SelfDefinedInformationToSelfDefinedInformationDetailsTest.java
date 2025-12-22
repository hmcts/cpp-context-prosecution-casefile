package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createSelfDefinedInformation;

import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.SelfDefinedInformationDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SelfDefinedInformationToSelfDefinedInformationDetailsTest extends ConverterBaseTest {

    @InjectMocks
    private SelfDefinedInformationToSelfDefinedInformationDetails converter;

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetails() {
        final SelfDefinedInformationDetails selfDefinedInformationDetails = converter.convert(createSelfDefinedInformation());
        assertSelfDefinedInformationDetails(selfDefinedInformationDetails);
    }

}