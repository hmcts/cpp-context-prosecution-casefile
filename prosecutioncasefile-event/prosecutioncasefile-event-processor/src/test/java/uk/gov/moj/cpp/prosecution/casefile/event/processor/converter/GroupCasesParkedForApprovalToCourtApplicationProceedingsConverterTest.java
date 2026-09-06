package uk.gov.moj.cpp.prosecution.casefile.event.processor.converter;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionList;
import uk.gov.moj.cpp.prosecution.casefile.domain.GroupProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.domain.ParamsVO;
import uk.gov.moj.cpp.prosecution.casefile.domain.ReferenceDataVO;
import uk.gov.moj.cpp.prosecution.casefile.event.GroupCasesParkedForApproval;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.GroupProsecution;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static java.lang.Boolean.TRUE;
import static java.time.LocalDate.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static uk.gov.justice.core.courts.BreachType.NOT_APPLICABLE;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.Jurisdiction.EITHER;
import static uk.gov.justice.core.courts.OffenceActiveOrder.OFFENCE;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.values;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails.caseDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation.personalInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

@ExtendWith(MockitoExtension.class)
public class GroupCasesParkedForApprovalToCourtApplicationProceedingsConverterTest {
    private static final UUID CASE_ID1 = randomUUID();
    private static final UUID CASE_ID2 = randomUUID();
    private static final String DEFENDANT_ID1 = randomUUID().toString();
    private static final String DEFENDANT_ID2 = randomUUID().toString();

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

    @InjectMocks
    private GroupCasesParkedForApprovalToCourtApplicationProceedingsConverter converter;

    @BeforeEach
    public void setup() {
        given(referenceDataQueryService.retrieveApplicationTypes()).willReturn(ImmutableList.of(getApplicationTypeForFirstHearing()));
    }

    @Test
    public void shouldConvertSuccessfullyWhenInputIsOk() {
        final GroupCasesParkedForApproval source = buildGroupCasesParkedForApproval();
        final BoxHearingRequest boxHearingRequest = boxHearingRequest();
        final CourtApplicationParty applicant = applicant();
        final List<CourtApplicationParty> respondents = respondents();
        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier();
        final List<uk.gov.justice.core.courts.Offence> courtApplicationOffences = courtApplicationOffences();


        given(prosecutionToBoxHearingRequestConverter.convert(any())).willReturn(boxHearingRequest);
        given(prosecutionCaseFileCaseDetailsToProsecutionCaseIdentifierConverter.convert(any(CaseDetails.class), any(Metadata.class))).willReturn(prosecutionCaseIdentifier);
        given(prosecutionCaseFileOffenceToCourtApplicationOffenceConverter.convert(anyList(), any(ParamsVO.class))).willReturn(courtApplicationOffences);
        given(prosecutionCaseFileDefendantToCourtApplicationPartyConverter.convert(anyList(), any(ReferenceDataVO.class), any(Channel.class))).willReturn(respondents);
        given(prosecutionCaseFileProsecutorToCourtApplicationPartyConverter.convert(any(), any(ParamsVO.class), any(Metadata.class))).willReturn(applicant);

        final InitiateCourtApplicationProceedings applicationProceedings = converter.convert(source, buildMetadata());

        assertThat(applicationProceedings.getBoxHearing(), is(boxHearingRequest));
        assertThat(applicationProceedings.getCourtHearing(), nullValue());
        assertThat(applicationProceedings.getSummonsApprovalRequired(), is(TRUE));

        final CourtApplication courtApplication = applicationProceedings.getCourtApplication();
        assertThat(courtApplication, notNullValue());
        assertThat(courtApplication.getApplicant(), is(applicant));
        assertThat(courtApplication.getRespondents(), is(respondents));
        assertThat(courtApplication.getSubject(), is(respondents.get(0)));
        assertThat(courtApplication.getCourtApplicationCases(), hasSize(1));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseIdentifier(), is(prosecutionCaseIdentifier));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getProsecutionCaseId(), is(CASE_ID1));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getOffences(), is(courtApplicationOffences));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getIsSJP(), is(false));
        assertThat(courtApplication.getCourtApplicationCases().get(0).getCaseStatus(), is("ACTIVE"));

        assertThat(courtApplication.getType(), is(notNullValue()));
        assertThat(courtApplication.getType().getLinkType(), is(LinkType.FIRST_HEARING));

    }

    private GroupCasesParkedForApproval buildGroupCasesParkedForApproval() {
        final List<GroupProsecutionWithReferenceData> groupProsecutions = asList(
                createGroupProsecutionWithReferenceData(CASE_ID1, DEFENDANT_ID1),
                createGroupProsecutionWithReferenceData(CASE_ID2, DEFENDANT_ID2));

        return GroupCasesParkedForApproval.groupCasesParkedForApproval()
                .withApplicationId(UUID.randomUUID())
                .withGroupProsecutionList(new GroupProsecutionList(groupProsecutions, UUID.randomUUID(), Channel.CIVIL))
                .build();
    }

    private GroupProsecutionWithReferenceData createGroupProsecutionWithReferenceData(final UUID caseId, final String defendantId){
        return new GroupProsecutionWithReferenceData(GroupProsecution.groupProsecution()
                .withCaseDetails(caseDetails()
                        .withCaseId(caseId)
                        .withInitiationCode(INITIATION_CODE)
                        .withProsecutorCaseReference(PROSECUTOR_CASE_REFERENCE)
                        .withOriginatingOrganisation(ORIGINATING_ORGANISATION)
                        .withCpsOrganisation(CPS_ORGANISATION)
                        .withSummonsCode(values("A", "W", "B", "E").next())
                        .withDateReceived(CASE_RECEIVED_DATE)
                        .build())
                .withDefendants(ImmutableList.of(defendant()
                        .withId(defendantId)
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
                        .build()))
                .withGroupId(randomUUID())
                .withIsGroupMaster(caseId.equals(CASE_ID1))
                .withIsGroupMember(true)
                .withIsCivil(true)
                .build());
    }

    private Metadata buildMetadata() {
        return metadataBuilder().withId(randomUUID()).withName(STRING.next()).build();
    }

    private BoxHearingRequest boxHearingRequest(){
        return BoxHearingRequest.boxHearingRequest().build();
    }

    private ProsecutionCaseIdentifier prosecutionCaseIdentifier(){
        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build();
    }

    private List<uk.gov.justice.core.courts.Offence> courtApplicationOffences(){
        return asList(uk.gov.justice.core.courts.Offence.offence().build());
    }

    private List<CourtApplicationParty> respondents(){
        return asList(CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .build());
    }

    private CourtApplicationParty applicant(){
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .build();
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

}