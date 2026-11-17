package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingDateTimeType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingRequest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileDefendantToCourtApplicationPartyConverterTest {

    @Mock
    private ProsecutionCaseFileToCCLegalEntityDefendantConverter prosecutionCaseFileToCCLegalEntityDefendantConverter;

    @Mock
    private ProsecutionCaseToCCPersonDefendantConverter prosecutionCaseToCCPersonDefendantConverter;

    @Mock
    private ReferenceDataVO referenceDataVO;

    @Mock
    private PersonDefendant personDefendant;

    @InjectMocks
    private ProsecutionCaseFileDefendantToCourtApplicationPartyConverter converter;

    @Test
    public void convertAdultDefendant() {

        final Defendant defendant = getDefendant(false, randomAlphabetic(15), null);
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO, Channel.SPI);

        verify(prosecutionCaseToCCPersonDefendantConverter).convert(defendant, referenceDataVO);
        verify(prosecutionCaseFileToCCLegalEntityDefendantConverter).convert(defendant);
        assertThat(courtApplicationParties, hasSize(1));
        assertThat(courtApplicationParties.get(0).getId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getSummonsRequired(), is(true));
        assertThat(courtApplicationParties.get(0).getNotificationRequired(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getMasterDefendantId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getProsecutionAuthorityReference(), is(defendant.getProsecutorDefendantReference()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getPersonDefendant(), is(personDefendant));
    }

    @Test
    public void convertYouthDefendant() {

        final Defendant defendant = getDefendant(true, randomAlphabetic(15), null);
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO, Channel.SPI);

        verify(prosecutionCaseToCCPersonDefendantConverter).convert(defendant, referenceDataVO);
        verify(prosecutionCaseFileToCCLegalEntityDefendantConverter).convert(defendant);
        assertThat(courtApplicationParties, hasSize(1));
        assertThat(courtApplicationParties.get(0).getId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getSummonsRequired(), is(true));
        assertThat(courtApplicationParties.get(0).getNotificationRequired(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getMasterDefendantId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(true));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getProsecutionAuthorityReference(), is(defendant.getProsecutorDefendantReference()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getPersonDefendant(), is(personDefendant));
    }

    @Test
    public void shouldSetDefendantProsecutorDefendantReferenceInCourtApplicationForNonMCC() {

        final Defendant defendant = getDefendant(false, randomAlphabetic(15), null);
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO, Channel.SPI);

        verify(prosecutionCaseToCCPersonDefendantConverter).convert(defendant, referenceDataVO);
        verify(prosecutionCaseFileToCCLegalEntityDefendantConverter).convert(defendant);
        assertThat(courtApplicationParties, hasSize(1));
        assertThat(courtApplicationParties.get(0).getId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getSummonsRequired(), is(true));
        assertThat(courtApplicationParties.get(0).getNotificationRequired(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getMasterDefendantId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getProsecutionAuthorityReference(), is(defendant.getProsecutorDefendantReference()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getPersonDefendant(), is(personDefendant));
    }

    @Test
    public void shouldSetAsnInCourtApplicationForMCC() {

        final Defendant defendant = getDefendant(false, null, "ASN1234");
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO, Channel.MCC);

        verify(prosecutionCaseToCCPersonDefendantConverter).convert(defendant, referenceDataVO);
        verify(prosecutionCaseFileToCCLegalEntityDefendantConverter).convert(defendant);
        assertThat(courtApplicationParties, hasSize(1));
        assertThat(courtApplicationParties.get(0).getId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getSummonsRequired(), is(true));
        assertThat(courtApplicationParties.get(0).getNotificationRequired(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getMasterDefendantId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getProsecutionAuthorityReference(), is(defendant.getAsn()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getPersonDefendant(), is(personDefendant));
    }

    @Test
    public void shouldNotSetAsnInCourtApplicationForNonMCC() {

        final Defendant defendant = getDefendant(false, null, "ASN1234");
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO, Channel.SPI);

        verify(prosecutionCaseToCCPersonDefendantConverter).convert(defendant, referenceDataVO);
        verify(prosecutionCaseFileToCCLegalEntityDefendantConverter).convert(defendant);
        assertThat(courtApplicationParties, hasSize(1));
        assertThat(courtApplicationParties.get(0).getId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getSummonsRequired(), is(true));
        assertThat(courtApplicationParties.get(0).getNotificationRequired(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getMasterDefendantId().toString(), is(defendant.getId()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(false));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getProsecutionAuthorityReference(), is(defendant.getProsecutorDefendantReference()));
        assertThat(courtApplicationParties.get(0).getMasterDefendant().getPersonDefendant(), is(personDefendant));
    }

    /**
     * DD-43173 AC-013: with no initialHearing the youth check falls back to the case's found hearing date.
     */
    @Test
    public void shouldMarkDefendantAsYouthFromTheFoundHearingDateWhenThereIsNoInitialHearing() {
        final LocalDate hearingDate = LocalDate.now().plusDays(5);
        final Defendant defendant = getFindAHearingDefendant(hearingDate.minusYears(18).plusDays(5));
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO,
                Channel.MCC, fixedFoundHearing(hearingDate));

        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(true));
    }

    /**
     * AC-013: an adult at the found hearing date is not a youth.
     */
    @Test
    public void shouldNotMarkDefendantAsYouthFromTheFoundHearingDateWhenTheyAreAnAdultOnThatDate() {
        final LocalDate hearingDate = LocalDate.now().plusDays(5);
        final Defendant defendant = getFindAHearingDefendant(hearingDate.minusYears(18).minusDays(1));
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO,
                Channel.MCC, fixedFoundHearing(hearingDate));

        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(false));
    }

    /**
     * AC-013: WEEK_COMMENCING found hearings take their date from weekCommencingDate.startDate.
     */
    @Test
    public void shouldMarkDefendantAsYouthFromTheWeekCommencingStartDate() {
        final LocalDate weekCommencing = LocalDate.now().plusDays(5);
        final Defendant defendant = getFindAHearingDefendant(weekCommencing.minusYears(18).plusDays(5));
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final HearingRequest foundHearing = HearingRequest.hearingRequest()
                .withHearingDateTimeType(HearingDateTimeType.WEEK_COMMENCING)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(weekCommencing.toString())
                        .withDuration(1)
                        .build())
                .build();

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO, Channel.MCC, foundHearing);

        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(true));
    }

    /**
     * No hearing date from either source leaves the youth flag false, as it is today.
     */
    @Test
    public void shouldNotMarkDefendantAsYouthWhenThereIsNeitherAnInitialHearingNorAFoundHearing() {
        final Defendant defendant = getFindAHearingDefendant(LocalDate.now().minusYears(10));
        when(prosecutionCaseToCCPersonDefendantConverter.convert(defendant, referenceDataVO)).thenReturn(personDefendant);

        final List<CourtApplicationParty> courtApplicationParties = converter.convert(singletonList(defendant), referenceDataVO, Channel.MCC);

        assertThat(courtApplicationParties.get(0).getMasterDefendant().getIsYouth(), is(false));
    }

    private HearingRequest fixedFoundHearing(final LocalDate hearingDate) {
        return HearingRequest.hearingRequest()
                .withHearingDateTimeType(HearingDateTimeType.FIXED)
                .withEarliestStartDateTime(hearingDate.atStartOfDay(ZoneId.systemDefault()))
                .build();
    }

    private Defendant getFindAHearingDefendant(final LocalDate dateOfBirth) {
        return Defendant.defendant()
                .withId(randomUUID().toString())
                .withProsecutorDefendantReference(randomAlphabetic(15))
                .withIndividual(individual()
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(dateOfBirth)
                                .build())
                        .build())
                .build();
    }

    private Defendant getDefendant(final boolean isYouth, final String prosecutorDefendantReference, final String asn) {
        final LocalDate dateOfHearing  = LocalDate.now().plusDays(5);
        final LocalDate defendantDateOfBirth = isYouth ? dateOfHearing.minusYears(18).plusDays(5) : dateOfHearing.minusYears(40).plusDays(5);

        return Defendant.defendant()
                .withId(randomUUID().toString())
                .withAsn(asn)
                .withProsecutorDefendantReference(prosecutorDefendantReference)
                .withInitialHearing(initialHearing()
                        .withDateOfHearing(dateOfHearing.toString())
                        .build())
                .withIndividual(individual()
                        .withSelfDefinedInformation(selfDefinedInformation()
                                .withDateOfBirth(defendantDateOfBirth)
                                .build())
                        .build())
                .build();
    }
}