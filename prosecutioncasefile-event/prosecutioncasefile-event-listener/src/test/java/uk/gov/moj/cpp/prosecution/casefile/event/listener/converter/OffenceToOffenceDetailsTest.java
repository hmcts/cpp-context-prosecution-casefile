package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.OFFENCE_ID;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createOffence;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createOffenceWithoutId;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.OffenceDetails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OffenceToOffenceDetailsTest extends ConverterBaseTest{

    @Mock
    private OffenceToOffenceDetails.OffenceIdGenerator idGenerator;

    @InjectMocks
    private OffenceToOffenceDetails converter;

    @Test
    public void testConvertDefendantToDefendantDetails() {
        convertOffenceAndAssertOffenceDetails(createOffence());
        verify(idGenerator, never()).generateId();
    }

    @Test
    public void testConvertDefendantWithoutIdToDefendantDetails() {
        when(idGenerator.generateId()).thenReturn(OFFENCE_ID);
        convertOffenceAndAssertOffenceDetails(createOffenceWithoutId());
        verify(idGenerator).generateId();
    }

    private void convertOffenceAndAssertOffenceDetails(final Offence offence){
        final OffenceDetails offenceDetails = converter.convert(offence);
        assertOffenceDetails(offenceDetails);
    }

}
