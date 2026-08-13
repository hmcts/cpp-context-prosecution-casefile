package uk.gov.moj.cpp.prosecution.casefile.event.listener.converter;


import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecution;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecutionWithFeeStatus;
import static uk.gov.moj.cpp.prosecution.casefile.event.listener.converter.TestDataProvider.createProsecutionWithLanguage;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CaseDetails;
import uk.gov.moj.cpp.prosecutioncasefile.persistence.entity.CivilFees;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionReceived;

import java.util.Set;

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

    @Test
    public void shouldPersistCivilFeesWhenFeeStatusIsPresent() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("PAID", "PAID"));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), hasSize(2));
    }

    @Test
    public void shouldNotPersistCivilFeesWhenFeeStatusIsNull() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus(null, null));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), is(nullValue()));
    }

    @Test
    public void shouldNotPersistCivilFeesWhenFeeStatusIsBlank() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("   ", ""));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), is(nullValue()));
    }

    @Test
    public void shouldNotPersistCivilFeesWhenFeeStatusIsNotApplicable() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("NOT_APPLICABLE", "not_applicable"));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());

        assertThat(caseDetails.getCivilFees(), is(nullValue()));
    }

    @Test
    public void shouldPersistOnlyApplicableCivilFeeWhenContestedFeeStatusIsNotApplicable() {
        final SjpProsecutionReceived prosecutionReceived = new SjpProsecutionReceived(randomUUID(),
                createProsecutionWithFeeStatus("PAID", "NOT_APPLICABLE"));

        final CaseDetails caseDetails = converter.convert(prosecutionReceived.getProsecution());
        final Set<CivilFees> civilFees = caseDetails.getCivilFees();

        assertThat(civilFees, hasSize(1));
        assertThat(civilFees.iterator().next().getFeeStatus(), is("PAID"));
    }
}