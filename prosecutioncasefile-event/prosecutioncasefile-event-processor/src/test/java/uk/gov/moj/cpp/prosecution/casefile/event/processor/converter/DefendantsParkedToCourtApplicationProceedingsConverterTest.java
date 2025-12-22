package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.of;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.justice.core.courts.BreachType.NOT_APPLICABLE;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.Jurisdiction.EITHER;
import static uk.gov.justice.core.courts.OffenceActiveOrder.OFFENCE;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.values;
import static uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval.defendantsParkedForSummonsApplicationApproval;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CIVIL;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantsParkedToCourtApplicationProceedingsConverterTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String INITIATION_CODE = "S";
    private static final String PROSECUTOR_CASE_REFERENCE = "Prosecutor Case Reference";
    private static final String ORIGINATING_ORGANISATION = "Originating Organisation";
    private static final String CPS_ORGANISATION = "A30AB00";
    private static final String OFFENCE_CODE = "A00PCD7073";
    private static final LocalDate ARREST_DATE = LocalDate.now().minusMonths(4);
    private static final LocalDate OFFENCE_COMMITTED_DATE = LocalDate.now();
    private static final LocalDate OFFENCE_CHARGE_DATE = LocalDate.now().minusMonths(4);
    private static final String SURNAME = "Bloggs";
    private static final LocalDate BIRTH_DATE = of(1991, 5, 5);
    private static final String COURT_HEARING_LOCATION = "B016771";
    private static final String DATE_OF_HEARING = "2050-10-03";
    private static final String TIME_OF_HEARING = "09:05:01.001";
    private static final String CUSTODY_STATUS = "B";
    private static final String FORENAME = "Joe";
    private static final LocalDate CASE_RECEIVED_DATE = of(2020, 1, 1);

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Mock
    private ProsecutionToBoxHearingRequestConverter prosecutionToBoxHearingRequestConverter;

    @Mock
    private ProsecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter prosecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter;

    @Mock
    private ProsecutionCaseFileOffenceToCourtApplicationOffenceConverter prosecutionCaseFileOffenceToCourtApplicationOffenceConverter;

    @Mock
    private ProsecutionCaseFileDefendantToCourtApplicationPartyConverter prosecutionCaseFileDefendantToCourtApplicationPartyConverter;

    @Mock
    private ProsecutionCaseFileProsecutorToCourtApplicationPartyConverter prosecutionCaseFileProsecutorToCourtApplicationPartyConverter;

    @Mock
    private BoxHearingRequest boxHearingRequest;

    @Mock
    private List<uk.gov.justice.core.courts.Offence> courtApplicationOffences;

    @Mock
    private List<CourtApplicationParty> respondents;

    @Mock
    private CourtApplicationParty respondent;

    @Mock
    private CourtApplicationParty applicant;

    @Mock
    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    @InjectMocks
    private DefendantsParkedToCourtApplicationProceedingsConverter target;

    @BeforeEach
    public void setup() {
        given(respondents.get(0)).willReturn(respondent);
        given(respondent.getId()).willReturn(fromString(DEFENDANT_ID));
        given(referenceDataQueryService.getApplicationType(DefendantsParkedToCourtApplicationProceedingsConverter.FIRST_HEARING_APPLICATION_ID)).willReturn(getApplicationTypeForFirstHearing());
    }

    @ParameterizedTest
    @MethodSource("provideParametersForCCCaseTest")
    void shouldConvertSuccessfullyWhenInputIsOk(Channel channel, String paymentRef, String actualFeeStatus, FeeStatus expectedFeeStatus) {
        final DefendantsParkedForSummonsApplicationApproval source = buildDefendantsParkedForSummonsApplicationApproval(channel, paymentRef, actualFeeStatus);
        final Prosecution prosecution = source.getProsecutionWithReferenceData().getProsecution();
        final CaseDetails caseDetails = prosecution.getCaseDetails();
        final List<Offence> offences = prosecution.getDefendants().stream().flatMap(defendant -> defendant.getOffences().stream()).collect(toList());
        given(prosecutionToBoxHearingRequestConverter.convert(prosecution)).willReturn(boxHearingRequest);
        given(prosecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter.convert(eq(caseDetails), any(Metadata.class))).willReturn(prosecutionCaseIdentifier);
        given(prosecutionCaseFileOffenceToCourtApplicationOffenceConverter.convert(eq(offences), any(ParamsVO.class))).willReturn(courtApplicationOffences);
        given(prosecutionCaseFileDefendantToCourtApplicationPartyConverter.convert(eq(prosecution.getDefendants()), any(ReferenceDataVO.class), any(Channel.class))).willReturn(respondents);
        given(prosecutionCaseFileProsecutorToCourtApplicationPartyConverter.convert(eq(caseDetails.getProsecutor()), any(ParamsVO.class), any(Metadata.class))).willReturn(applicant);

        final InitiateCourtApplicationProceedings applicationProceedings = target.convert(source, buildMetadata());

        assertThat(applicationProceedings.getBoxHearing(), is(boxHearingRequest));
        assertThat(applicationProceedings.getCourtHearing(), nullValue());
        assertThat(applicationProceedings.getSummonsApprovalRequired(), is(TRUE));

        final CourtApplication courtApplication = applicationProceedings.getCourtApplication();
        assertThat(courtApplication, notNullValue());
        assertThat(courtApplication.getApplicant(), is(applicant));
        assertThat(courtApplication.getRespondents(), is(respondents));
        assertThat(courtApplication.getSubject(), is(respondent));
        assertThat(courtApplication.getCourtApplicationCases(), hasSize(1));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseIdentifier(), is(prosecutionCaseIdentifier));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseId(), is(CASE_ID));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getOffences(), is(courtApplicationOffences));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getIsSJP(), is(false));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getCaseStatus(), is("ACTIVE"));
        assertThat(courtApplication.getCourtApplicationPayment().getFeeStatus(), is(expectedFeeStatus));

        if (channel == CIVIL) {
            assertThat(courtApplication.getCourtCivilApplication().getIsCivil(), is(true));
        } else {
            assertThat(courtApplication.getCourtCivilApplication().getIsCivil(), is(false));
        }

        assertThat(courtApplication.getType().getType(), is("Application for first hearing summons for criminal case"));
        assertThat(courtApplication.getType().getLinkType(), is(LinkType.FIRST_HEARING));
    }

    private DefendantsParkedForSummonsApplicationApproval buildDefendantsParkedForSummonsApplicationApproval(Channel channel, String paymentRef, final String actualFeeStatuss) {

        boolean isCivil = (channel == CIVIL);

        return defendantsParkedForSummonsApplicationApproval()
                .withProsecutionWithReferenceData(new ProsecutionWithReferenceData(prosecution()
                        .withCaseDetails(caseDetails()
                                .withCaseId(CASE_ID)
                                .withInitiationCode(INITIATION_CODE)
                                .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                                .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                                .withCpsOrganisation(CPS_ORGANISATION)
                                .withSummonsCode(values("A", "W", "B", "E").next())
                                .withDateReceived(CASE_RECEIVED_DATE)
                                .withFeeStatus(actualFeeStatuss)
                                .withPaymentReference(paymentRef)
                                .build())
                        .withChannel(channel)
                        .withDefendants(ImmutableList.of(defendant()
                                .withId(DEFENDANT_ID)
                                .withIndividual(individual()
                                        .withPersonalInformation(personalInformation()
                                                .withFirstName(FORENAME)
                                                .withLastName(SURNAME).build())
                                        .withSelfDefinedInformation(selfDefinedInformation()
                                                .withDateOfBirth(BIRTH_DATE)
                                                .build())
                                        .withBailConditions("bailConditions")
                                        .build())
                                .withInitialHearing(initialHearing()
                                        .withDateOfHearing(DATE_OF_HEARING)
                                        .withTimeOfHearing(TIME_OF_HEARING)
                                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                                        .build())
                                .withCustodyStatus(CUSTODY_STATUS)
                                .withOffences(singletonList(offence()
                                        .withOffenceId(randomUUID())
                                        .withOffenceSequenceNumber(1)
                                        .withArrestDate(ARREST_DATE)
                                        .withOffenceCode(OFFENCE_CODE)
                                        .withOffenceCommittedDate(OFFENCE_COMMITTED_DATE)
                                        .withChargeDate(OFFENCE_CHARGE_DATE)
                                        .withOffenceDateCode(2)
                                        .build()))
                                .withInitiationCode(INITIATION_CODE)
                                .build()))
                        .withIsCivil(isCivil)
                        .build()))
                .build();
    }

    private Metadata buildMetadata() {
        return metadataBuilder().withId(randomUUID()).withName(STRING.next()).build();
    }

    private CourtApplicationType getApplicationTypeForFirstHearing() {
        return courtApplicationType()
                .withId(fromString("cb05a560-48f9-415d-b230-55b78f8cb4a8"))
                .withType("Application for first hearing summons for criminal case")
                .withCategoryCode("CO")
                .withLinkType(LinkType.FIRST_HEARING)
                .withJurisdiction(EITHER)
                .withSummonsTemplateType(SummonsTemplateType.FIRST_HEARING)
                .withBreachType(NOT_APPLICABLE)
                .withAppealFlag(false)
                .withApplicantAppellantFlag(false)
                .withPleaApplicableFlag(false)
                .withCommrOfOathFlag(false)
                .withCourtOfAppealFlag(false)
                .withCourtExtractAvlFlag(false)
                .withProsecutorThirdPartyFlag(false)
                .withSpiOutApplicableFlag(false)
                .withOffenceActiveOrder(OFFENCE)
                .build();
    }

    private static Stream<Arguments> provideParametersForCCCaseTest() {
        //Stream of paymentref, actualFeeStatus, expectedFeeStatus
        return Stream.of(
                Arguments.of(Channel.MCC, null, null, FeeStatus.NOT_APPLICABLE),
                Arguments.of(Channel.MCC, "paymentRef01", null, FeeStatus.SATISFIED),
                Arguments.of(Channel.MCC, "paymentRef02", FeeStatus.SATISFIED.name(), FeeStatus.SATISFIED),
                Arguments.of(Channel.SPI, null, null, FeeStatus.NOT_APPLICABLE),
                Arguments.of(Channel.SPI, "paymentRef01", null, FeeStatus.SATISFIED),
                Arguments.of(Channel.SPI, "paymentRef02", FeeStatus.SATISFIED.name(), FeeStatus.SATISFIED),
                Arguments.of(Channel.CPPI, null, null, FeeStatus.NOT_APPLICABLE),
                Arguments.of(Channel.CPPI, "paymentRef01", null, FeeStatus.SATISFIED),
                Arguments.of(Channel.CPPI, "paymentRef02", FeeStatus.SATISFIED.name(), FeeStatus.SATISFIED),
                Arguments.of(Channel.CIVIL, null, null, FeeStatus.OUTSTANDING),
                Arguments.of(Channel.CIVIL, "paymentRef01", null, FeeStatus.SATISFIED),
                Arguments.of(Channel.CIVIL, "paymentRef02", FeeStatus.SATISFIED.name(), FeeStatus.SATISFIED)
        );
    }
}
