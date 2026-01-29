package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;


import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecution;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecutionWithLanguage;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionReceivedToCaseTest extends ConverterBaseTest{

    @InjectMocks
    private ProsecutionReceivedToCase converter;

    @Spy
    @InjectMocks
    private DefendantToDefendantDetails defendantToDefendantDetail;

    @Spy
    private OffenceToOffenceDetails offenceToOffenceDetails;

    @Spy
    @InjectMocks
    private CaseDetailsToCivilFees caseDetailsToCivilFees;

    @Spy
    @InjectMocks
    private PersonalInformationToPersonalInformationDetails personalInformationToPersonalInformationDetails;

    @Spy
    private SelfDefinedInformationToSelfDefinedInformationDetails selfDefinedInformationToSelfDefinedInformationDetails;

    @Spy
    private AddressToAddressDetails addressToAddressDetails;

    @Spy
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetails() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecution());

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails);
    }

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndENGLISHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.ENGLISH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.E);
    }

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndWALSHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.WELSH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.W);
    }
}