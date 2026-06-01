package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;


import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecution;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecutionWithLanguage;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import org.junit.jupiter.api.BeforeEach;
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
    private DefendantToDefendantDetails defendantToDefendantDetail;

    @Spy
    private OffenceToOffenceDetails offenceToOffenceDetails;

    @Spy
    private CaseDetailsToCivilFees caseDetailsToCivilFees;

    @Spy
    private PersonalInformationToPersonalInformationDetails personalInformationToPersonalInformationDetails;

    @Spy
    private SelfDefinedInformationToSelfDefinedInformationDetails selfDefinedInformationToSelfDefinedInformationDetails;

    @Spy
    private AddressToAddressDetails addressToAddressDetails;

    @Spy
    private ContactDetailsToContactDetailsEntity contactDetailsToContactDetailsEntity;

    @BeforeEach
    void setup() {
        setField(personalInformationToPersonalInformationDetails, "addressToAddressDetails", addressToAddressDetails);
        setField(personalInformationToPersonalInformationDetails, "contactDetailsToContactDetailsDetails", contactDetailsToContactDetailsEntity);
        setField(defendantToDefendantDetail, "personalInformationToPersonalInformationDetails", personalInformationToPersonalInformationDetails);
        setField(defendantToDefendantDetail, "selfDefinedInformationToSelfDefinedInformationDetails", selfDefinedInformationToSelfDefinedInformationDetails);
        setField(defendantToDefendantDetail, "offenceToOffenceDetails", offenceToOffenceDetails);
        setField(defendantToDefendantDetail, "addressToAddressDetails", addressToAddressDetails);
    }

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetails() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecution());

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails);
    }

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndENGLISHAndWELSHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.ENGLISH, Language.WELSH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.E, Language.W);
    }

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndWALSHAndEnglishLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.WELSH, Language.ENGLISH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.W, Language.E);
    }

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndENGLISHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.ENGLISH, Language.ENGLISH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.E, Language.E);
    }

    @Test
    public void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndWALSHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.WELSH, Language.WELSH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.W, Language.W);
    }
}