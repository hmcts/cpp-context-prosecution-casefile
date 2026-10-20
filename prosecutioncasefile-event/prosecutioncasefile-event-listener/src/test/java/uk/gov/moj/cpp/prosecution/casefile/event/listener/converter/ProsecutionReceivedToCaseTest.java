package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecution;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecutionWithFeeStatus;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecutionWithLanguage;

import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import java.util.Set;

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
    void testConvertSelfDefinedInformationToSelfDefinedInformationDetails() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecution());

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails);
    }

    @Test
    void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndENGLISHAndWELSHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.ENGLISH, Language.WELSH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.E, Language.W);
    }

    @Test
    void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndWALSHAndEnglishLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.WELSH, Language.ENGLISH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.W, Language.E);
    }

    @Test
    void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndENGLISHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.ENGLISH, Language.ENGLISH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.E, Language.E);
    }

    @Test
    void testConvertSelfDefinedInformationToSelfDefinedInformationDetailsAndWALSHLanguageCheck() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(), createProsecutionWithLanguage(Language.WELSH, Language.WELSH));
        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        assertCaseDetails(caseDetails, Language.W, Language.W);
    }

    @Test
    void shouldPersistCivilFeesWhenFeeStatusIsPresent() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("PAID", "PAID", true));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), hasSize(2));
    }

    @Test
    void shouldNotPersistCivilFeesWhenFeeStatusIsNull() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus(null, null, true));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), is(nullValue()));
    }

    @Test
    void shouldNotPersistCivilFeesWhenFeeStatusIsEmpty() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("", "", true));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), is(nullValue()));
    }

    @Test
    void shouldPersistCivilFeesWhenFeeStatusIsNotApplicable() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("NOT_APPLICABLE", "not_applicable", true));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), hasSize(2));
    }

    @Test
    void shouldPersistBothCivilFeesWhenContestedFeeStatusIsNotApplicable() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("PAID", "NOT_APPLICABLE", true));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        final Set<CivilFees> civilFees = caseDetails.getCivilFees();

        assertThat(civilFees, hasSize(2));
    }

    @Test
    void shouldNotPersistCivilFeesWhenCaseIsNotCivil() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("PAID", "PAID", false));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), is(nullValue()));
    }
}