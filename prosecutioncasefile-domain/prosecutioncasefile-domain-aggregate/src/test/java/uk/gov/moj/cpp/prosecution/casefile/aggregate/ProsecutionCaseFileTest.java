package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.CPPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecution.prosecution;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATED_PROSECUTION;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfully.sjpCaseCreatedSuccessfully;
import static uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpCaseCreatedSuccessfullyWithWarnings.sjpCaseCreatedSuccessfullyWithWarnings;

import uk.gov.moj.cpp.prosecution.casefile.DocumentDetails;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Material;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProsecutorsReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.InitiationCode;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.prosecution.casefile.plea.json.schemas.PleadOnlinePcqVisited;
import uk.gov.moj.cpp.prosecution.casefile.refdata.defendant.DefendantRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.refdata.proscase.CaseRefDataEnricher;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cps.prosecutioncasefile.common.AddMaterialCommonV2;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseCreatedSuccessfully;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseFiltered;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CaseReferredToCourtRecorded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaPcqVisitedSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.OnlinePleaSubmitted;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseUnsupported;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SjpProsecutionRejected;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseFileTest {

    ProsecutionCaseFile prosecutionCaseFile;

    @BeforeEach
    public void setUp() {
        prosecutionCaseFile = new ProsecutionCaseFile();
    }

    @Mock
    private Instance<CaseRefDataEnricher> caseRefDataEnrichers;

    @Mock
    private Instance<DefendantRefDataEnricher> defendantRefDataEnrichers;

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Test
    public void shouldAcceptProsecutionWhenEventOfSjpCaseCreatedSuccessfully() {
        prosecutionCaseFile.apply(sjpCaseCreatedSuccessfully().build());

        assertThat(prosecutionCaseFile.isProsecutionAccepted(), is(true));
    }

    @Test
    public void shouldAcceptProsecutionWhenEventOfSjpCaseCreatedSuccessfullyWithWarnings() {
        prosecutionCaseFile.apply(sjpCaseCreatedSuccessfullyWithWarnings().build());

        assertThat(prosecutionCaseFile.isProsecutionAccepted(), is(true));
    }

    @Test
    public void shouldAcceptProsecutionWhenEventOfCaseCreatedSuccessfully() {
        prosecutionCaseFile.apply(CaseCreatedSuccessfully.caseCreatedSuccessfully().build());

        assertThat(prosecutionCaseFile.isProsecutionAccepted(), is(true));
    }

    @Test
    public void shouldRejectProsecutionWhenCaseReferredToCourtRecorded() {
        ProsecutionCaseFile prosecutionCaseFile = new ProsecutionCaseFile();
        final String urn = "TFL7421758";
        final UUID caseId = randomUUID();
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withChannel(CPPI)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(caseId)
                        .withProsecutorCaseReference(urn)
                        .withProsecutor(Prosecutor.prosecutor().withReferenceData(
                                ProsecutorsReferenceData.prosecutorsReferenceData()
                                        .withSjpFlag(true)
                                        .withAocpApproved(true)
                                        .build())
                                .build())
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withId(randomUUID().toString())
                                .withOffences(Arrays.asList(Offence.offence()
                                                .withOffenceId(randomUUID())
                                                .withOffenceCode("offenceCode")
                                                .withOffenceSequenceNumber(1)
                                        .build()))
                        .build()))
                .build());

        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());

        prosecutionCaseFile.apply(CaseReferredToCourtRecorded.caseReferredToCourtRecorded().withCaseId(caseId).withReferralReasonId(randomUUID()).build());
        Stream<Object> result = prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, newArrayList(caseRefDataEnrichers.iterator()), newArrayList(defendantRefDataEnrichers.iterator()), referenceDataQueryService);

        final SjpProsecutionRejected sjpProsecutionRejected = (SjpProsecutionRejected) result.findFirst().get();
        final Optional<Problem> duplicatedProblem = sjpProsecutionRejected.getErrors().stream().filter(e -> e.getCode().equals(DUPLICATED_PROSECUTION.name())).findFirst();
        assertThat(duplicatedProblem.get().getValues().get(0).getKey(), is("urn"));
        assertThat(duplicatedProblem.get().getValues().get(0).getValue(), is(urn));
    }


    @Test
    public void shouldRaiseCaseFilteredEventWhenCaseFilterCommandReceived() {

        final UUID caseId = randomUUID();

        final Stream<Object> objectStream = prosecutionCaseFile.filterCase(caseId);
        final List<Object> eventList = objectStream.collect(Collectors.toList());

        assertThat(eventList.get(0), instanceOf(CaseFiltered.class));

        CaseFiltered caseFiltered = (CaseFiltered) eventList.get(0);
        assertThat(caseFiltered.getCaseId(), is(caseId));
        assertThat(caseFiltered.getMaterials(), is(empty()));

        assertThat(prosecutionCaseFile.isCaseFiltered(), is(true));
    }


    @Test
    public void shouldRaiseCaseFilteredEventWithMaterialWhenCaseFilterCommandReceivedAndMaterialPending() {

        final UUID caseId = randomUUID();
        final UUID fileStoreId = randomUUID();
        Material material = Material.material().withFileStoreId(fileStoreId).build();
        DocumentDetails documentDetails = new DocumentDetails("documentId", 1, "sectionCode");
        prosecutionCaseFile.addCpsMaterial(caseId, null, null, material, null, null, documentDetails);
        final Stream<Object> objectStream = prosecutionCaseFile.filterCase(caseId);


        final List<Object> eventList = objectStream.collect(Collectors.toList());

        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(CaseFiltered.class));

        CaseFiltered caseFiltered = (CaseFiltered) eventList.get(0);
        assertThat(caseFiltered.getCaseId(), is(caseId));
        assertThat(caseFiltered.getMaterials().size(), is(1));
        assertThat(caseFiltered.getMaterials().get(0).getFileStoreId(), is(fileStoreId));

        assertThat(prosecutionCaseFile.isCaseFiltered(), is(true));
    }

    @Test
    public void shouldRaiseCaseFilteredEventWithMaterialWhenCaseFilterCommandReceivedAndMaterialPendingV2() {

        final UUID caseId = randomUUID();
        final UUID fileStoreId = randomUUID();
        prosecutionCaseFile.addMaterialV2(AddMaterialCommonV2.addMaterialCommonV2()
                .withMaterial(fileStoreId)
                .withCaseId(caseId)
                .build(), null);
        final Stream<Object> objectStream = prosecutionCaseFile.filterCase(caseId);


        final List<Object> eventList = objectStream.collect(Collectors.toList());

        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(CaseFiltered.class));

        CaseFiltered caseFiltered = (CaseFiltered) eventList.get(0);
        assertThat(caseFiltered.getCaseId(), is(caseId));
        assertThat(caseFiltered.getMaterials().size(), is(1));
        assertThat(caseFiltered.getMaterials().get(0).getFileStoreId(), is(fileStoreId));

        assertThat(prosecutionCaseFile.isCaseFiltered(), is(true));
    }

    @Test
    public void shouldRejectProsecutionWhenCaseReferredToCourtRecordedFromSJP() {
        ProsecutionCaseFile prosecutionCaseFile = new ProsecutionCaseFile();
        final String urn = "TFL7421758";
        final UUID caseId = randomUUID();
        final ProsecutionWithReferenceData prosecutionWithReferenceData = new ProsecutionWithReferenceData(prosecution()
                .withChannel(SPI)
                .withCaseDetails(CaseDetails.caseDetails()
                        .withCaseId(caseId)
                        .withProsecutorCaseReference(urn)
                        .withProsecutor(Prosecutor.prosecutor().withReferenceData(ProsecutorsReferenceData.prosecutorsReferenceData().withSjpFlag(true).build()).build())
                        .build())
                .withDefendants(Arrays.asList(Defendant.defendant().withId(randomUUID().toString()).build()))
                .build());

        when(caseRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());
        when(defendantRefDataEnrichers.iterator()).thenReturn(Collections.emptyIterator());

        prosecutionCaseFile.apply(CaseReferredToCourtRecorded.caseReferredToCourtRecorded().withCaseId(caseId).withReferralReasonId(randomUUID()).build());
        Stream<Object> result = prosecutionCaseFile.receiveSjpProsecution(prosecutionWithReferenceData, newArrayList(caseRefDataEnrichers.iterator()), newArrayList(defendantRefDataEnrichers.iterator()), referenceDataQueryService);

        final ProsecutionCaseUnsupported prosecutionCaseUnsupported = (ProsecutionCaseUnsupported) result.findFirst().get();
        assertThat(prosecutionCaseUnsupported.getErrorMessage(), is("Multiple Defendants Found"));
    }

    @Test
    public void shouldHandlePleadOnline(){
        final UUID caseId = randomUUID();
        final PleadOnline pleadOnline = PleadOnline.pleadOnline()
                .withCaseId(caseId)
                .withInitiationCode(InitiationCode.J)
                .build();
        Stream<Object> result = prosecutionCaseFile.pleadOnline(caseId, pleadOnline, ZonedDateTime.now(),randomUUID());
        final OnlinePleaSubmitted onlinePleaSubmitted =(OnlinePleaSubmitted)result.findFirst().get();
        assertThat(onlinePleaSubmitted.getCaseId(), is(caseId));
    }

    @Test
    public void shouldHandlePleadOnlinePcqVisited(){
        final UUID caseId = randomUUID();
        final PleadOnlinePcqVisited pleadOnlinePcqVisited = PleadOnlinePcqVisited.pleadOnlinePcqVisited()
                .withCaseId(caseId)
                .withDefendantId(randomUUID())
                .withUrn("urn")
                .build();
        Stream<Object> result = prosecutionCaseFile.pleadOnlinePcqVisited(caseId, pleadOnlinePcqVisited, ZonedDateTime.now(),randomUUID());
        final OnlinePleaPcqVisitedSubmitted onlinePleaPcqVisitedSubmitted =(OnlinePleaPcqVisitedSubmitted)result.findFirst().get();
        assertThat(onlinePleaPcqVisitedSubmitted.getCaseId(), is(caseId));
    }

}