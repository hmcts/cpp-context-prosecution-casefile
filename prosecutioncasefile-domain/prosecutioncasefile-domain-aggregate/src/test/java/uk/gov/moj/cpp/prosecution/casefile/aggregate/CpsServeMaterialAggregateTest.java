package uk.gov.moj.cpp.prosecution.casefile.aggregate;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.prosecution.casefile.DateUtil.convertToLocalDate;
import static uk.gov.moj.cpp.prosecution.casefile.domain.FormValidationResult.formValidationResult;
import static uk.gov.moj.cpp.prosecution.casefile.utils.FileUtil.readJson;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.CASE_URN_NOT_FOUND;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CpsCaseContact;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantParentGuardian;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantTrialRepresentative;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OffenceReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.DefenceService;
import uk.gov.moj.cpp.prosecution.casefile.service.ProgressionService;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.CpsFormValidator;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.BcmDefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsDefendantOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CpsOffice;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defence;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Defendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PetFormData;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutorOffences;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PtphFormdefendants;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeBcmProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServeCotrProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePetProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.ReceivedCpsServePtphProcessed;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.SubmissionStatus;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.Witnesses;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsServeMaterialAggregateTest {

    private List<String> validOffences = asList("offenceCode");

    private CpsServeMaterialAggregate cpsServeMaterialAggregate;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Mock
    private CpsFormValidator cpsFormValidator;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private DefenceService defenceService;
    @Mock
    private ReferenceDataQueryService referenceDataQueryService;
    @Captor
    ArgumentCaptor<List<String>> captorArg1;
    @Captor
    ArgumentCaptor<Optional<String>> captorArg2;


    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        cpsServeMaterialAggregate = new CpsServeMaterialAggregate();
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseExists_ThereIsNoValidationError() throws FileNotFoundException {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();
        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }

    @Test
    public void shouldReturnReceivedCpsServeBcmProcessed_WhenProsecutionCaseExists_ThereIsNoValidationError() {
        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject bcmDefendants = readJson("json/bcmDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServeBcmJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-noValidationError.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefileBcm.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataBcm(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(bcmDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveBcm(processReceivedCpsServeBcmJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null);

        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServeBcmProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServeBcmProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServeBcmProcessed.getFormData().getBcmDefendants(), hasSize(1));
    }

    @Test
    public void shouldReturnReceivedCpsServePtphProcessed_WhenProsecutionCaseExists_ThereIsNoValidationError() {
        final JsonObject ptphFormData = readJson("json/ptphFormData.json", JsonObject.class);
        final JsonObject ptphDefendants = readJson("json/ptphDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePtphJson = readJson("json/public.stagingprosecutors.cps-serve-ptph-noValidationError.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefilePtph.case.json", JsonObject.class);
        OrganisationUnitReferenceData ouRefData = OrganisationUnitReferenceData.organisationUnitReferenceData()
                .withAddress1("address 1")
                .withAddress2("address 2")
                .withAddress3("address 3")
                .withAddress4("address 4")
                .withAddress5("address 5")
                .withPostcode("G11 1AB")
                .build();

        when(cpsFormValidator.validateAndRebuildingFormDataPtph(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(ptphDefendants)
                .withFormData(ptphFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePtph(processReceivedCpsServePtphJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                null, Optional.of(ouRefData),
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter);

        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = (ReceivedCpsServePtphProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePtphProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePtphProcessed.getFormDefendants(), hasSize(1));

        CpsOffice cpsOffice = receivedCpsServePtphProcessed.getPtphFormData().getContacts().getCpsOffice();
        assertThat(cpsOffice.getAddress1(), is("address 1"));
        assertThat(cpsOffice.getAddress2(), is("address 2"));
        assertThat(cpsOffice.getAddress3(), is("address 3"));
        assertThat(cpsOffice.getAddress4(), is("address 4"));
        assertThat(cpsOffice.getAddress5(), is("address 5"));
        assertThat(cpsOffice.getPostcode(), is("G11 1AB"));
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseDoesNotExist() {
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = null;

        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.ofNullable(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }


    @Test
    public void shouldReturnReceivedCpsServeBcmProcessed_WhenProsecutionCaseDoesNotExist() {
        final JsonObject processReceivedCpsServeBcmJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-noProsecutionCase.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = null;

        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveBcm(processReceivedCpsServeBcmJson,
                "SUCCESS",
                caseId,
                Optional.ofNullable(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null);

        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServeBcmProcessed) eventStream.findFirst().get();

        //assertOnReceivedCpsServeBcmProcessed(caseId, receivedCpsServeBcmProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePtphProcessed_WhenProsecutionCaseDoesNotExist() {
        final JsonObject ptphFormData = readJson("json/ptphFormData.json", JsonObject.class);
        final JsonObject ptphDefendants = readJson("json/ptphDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePtphJson = readJson("json/public.stagingprosecutors.cps-serve-ptph-noProsecutionCase.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = null;

        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePtph(processReceivedCpsServePtphJson,
                "SUCCESS",
                caseId,
                Optional.ofNullable(prosecutionCaseFileJson),
                cpsFormValidator,
                null,
                null,
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter);

        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = (ReceivedCpsServePtphProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePtphProcessed.getSubmissionStatus(), is(SubmissionStatus.REJECTED));

        final Optional<Problem> caseNotFoundProblem = receivedCpsServePtphProcessed.getErrors().stream().filter(e -> e.getCode().equals(CASE_URN_NOT_FOUND.name())).findFirst();
        assertThat(caseNotFoundProblem.get().getValues().get(0).getKey(), is("urn"));
        assertThat(caseNotFoundProblem.get().getValues().get(0).getValue(), is("45MD0000220"));
        assertThat(receivedCpsServePtphProcessed.getErrors(), hasSize(1));
    }


    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseExists_Asn_In_Event() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet_asn_only.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getTitle(), is("Mr"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getForename(), is("forename"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getForename2(), is("forename2"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getForename3(), is("forename3"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getEmail(), is("email@test.com"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getAddress1(), is("line 1"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getAddress2(), is("line 2"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getAddress3(), is("line 3"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getPostcode(), is("RG30 1UR"));
    }

    @Test
    void shouldReturnReceivedCpsServePetProcessed_acceptCasePet_WhenProsecutionCaseExists() {
        final UUID caseId = randomUUID();
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject progressionCaseFileJson = readJson("json/progression-prosecution-case.json", JsonObject.class);
        final HashSet<Object> pendingPetTypes = new HashSet<>();
        pendingPetTypes.add(CpsServeMaterialAggregate.PendingType.PET);
        ReflectionUtil.setField(cpsServeMaterialAggregate, "pendingSet", pendingPetTypes);
        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)

                .build());
        when(referenceDataQueryService.retrieveOffenceDataList(any(), any())).thenReturn(List.of(OffenceReferenceData.offenceReferenceData()
                .withCjsOffenceCode(randomUUID().toString())
                .build()));
        when(progressionService.getProsecutionCase(any())).thenReturn(progressionCaseFileJson);
        mockReceivedCpsServePetProcessed();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.acceptCasePet(caseId,
                new ProsecutionCaseFile(),
                Optional.of(progressionCaseFileJson.getJsonObject("prosecutionCase")),
                cpsFormValidator,
                referenceDataQueryService,
                progressionService,
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter,
                listToJsonArrayConverter, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();
        verify(referenceDataQueryService).retrieveOffenceDataList(captorArg1.capture(), captorArg2.capture());
        Optional<String> actualSowRef = captorArg2.getValue();
        assertThat(actualSowRef, is(Optional.empty()));
        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getTitle(), is("Mr"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getForename(), is("forename"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getForename2(), is("forename2"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getForename3(), is("forename3"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getEmail(), is("email@test.com"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getAddress1(), is("line 1"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getAddress2(), is("line 2"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getAddress3(), is("line 3"));
        assertThat(receivedCpsServePetProcessed.getCpsDefendantOffences().get(0).getParentGuardianForYouthDefendants().getPostcode(), is("RG30 1UR"));
    }

    private void mockReceivedCpsServePetProcessed() {
        final List<CpsDefendantOffences> cpsDefendantOffences = new ArrayList<>();
        mockCpsDefendantOffences(cpsDefendantOffences);
        ReflectionUtil.setField(cpsServeMaterialAggregate, "receivedCpsServePetProcessed",
                ReceivedCpsServePetProcessed.receivedCpsServePetProcessed()
                        .withPetFormData(PetFormData.petFormData()
                                .withDefence(Defence.defence()
                                        .withDefendants(List.of(Defendants.defendants()
                                                .withCpsOffences(List.of(CpsOffences.cpsOffences()
                                                        .withOffenceCode(randomUUID().toString())
                                                        .build()))
                                                .build()))
                                        .build())
                                .build())
                        .withSubmissionId(randomUUID())
                        .withIsYouth(true)
                        .withProsecutionCaseSubject(uk.gov.moj.cps.prosecutioncasefile.domain.event.ProsecutionCaseSubject.prosecutionCaseSubject().build())
                        .withCpsDefendantOffences(List.of())
                        .withSubmissionStatus(SubmissionStatus.SUCCESS)
                        .withReviewingLawyer(CpsCaseContact.cpsCaseContact().build())
                        .withCpsDefendantOffences(cpsDefendantOffences)
                        .build());
    }

    private static void mockCpsDefendantOffences(final List<CpsDefendantOffences> cpsDefendantOffences) {
        cpsDefendantOffences.add(CpsDefendantOffences.cpsDefendantOffences()
                .withAsn("1000NP0004444000203B")
                .withCpsDefendantId("d123ed32-ce29-11ec-9d64-0242ac120002")
                .withDateOfBirth(convertToLocalDate("1990-10-10"))
                .withForename("forename")
                .withForename2("forename2")
                .withForename3("forename3")
                .withParentGuardianForYouthDefendants(DefendantParentGuardian.defendantParentGuardian()
                        .withTitle("Mr")
                        .withForename("forename")
                        .withForename2("forename2")
                        .withForename3("forename3")
                        .withEmail("email@test.com")
                        .withAddress1("line 1")
                        .withAddress2("line 2")
                        .withAddress3("line 3")
                        .withPostcode("RG30 1UR")
                        .build())

                .withMatchingId(randomUUID())
                .withOrganisationName("organisation name")
                .withProsecutorDefendantId("prosecutor defandant ID")
                .withSurname("Smith")
                .withTitle("Mr")
                .build());
    }

    @Test
    public void shouldReturnReceivedCpsServeBcmProcessed_WhenProsecutionCaseExists_Asn_In_Event() {
        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject bcmDefendants = readJson("json/bcmDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServeBcmJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-asnOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefileBcm.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataBcm(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(bcmDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveBcm(processReceivedCpsServeBcmJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null);

        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServeBcmProcessed) eventStream.findFirst().get();

        assertOnReceivedCpsServeBcmProcessed(caseId, receivedCpsServeBcmProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePtphProcessed_WhenProsecutionCaseExists_Asn_In_Event() {
        final JsonObject ptphFormData = readJson("json/ptphFormData.json", JsonObject.class);
        final JsonObject ptphDefendants = readJson("json/ptphDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePtphJson = readJson("json/public.stagingprosecutors.cps-serve-ptph-asnOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefilePtph.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataPtph(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(ptphDefendants)
                .withFormData(ptphFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePtph(processReceivedCpsServePtphJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                null,
                Optional.empty(),
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter);

        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = (ReceivedCpsServePtphProcessed) eventStream.findFirst().get();

        assertOnReceivedCpsServePtphProcessedWithWitnessDetails(caseId, receivedCpsServePtphProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseExists_CpsDefendantId_In_Event() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet_cpsDefendantId_only.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }

    @Test
    public void shouldReturnReceivedCpsServeBcmProcessed_WhenProsecutionCaseExists_CpsDefendantId_In_Event() {
        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject bcmDefendants = readJson("json/bcmDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServeBcmJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-cpsDefendantIdOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefileBcm.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataBcm(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(bcmDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveBcm(processReceivedCpsServeBcmJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null);

        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServeBcmProcessed) eventStream.findFirst().get();

        assertOnReceivedCpsServeBcmProcessed(caseId, receivedCpsServeBcmProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePtphProcessed_WhenProsecutionCaseExists_CpsDefendantId_In_Event() {
        final JsonObject ptphFormData = readJson("json/ptphFormData.json", JsonObject.class);
        final JsonObject ptphDefendants = readJson("json/ptphDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePtphJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-cpsDefendantIdOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefilePtph.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataPtph(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(ptphDefendants)
                .withFormData(ptphFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePtph(processReceivedCpsServePtphJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                null,
                Optional.empty(),
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter);

        final ReceivedCpsServePtphProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServePtphProcessed) eventStream.findFirst().get();

        assertOnReceivedCpsServePtphProcessed(caseId, receivedCpsServeBcmProcessed);
        assertThat(receivedCpsServeBcmProcessed.getPtphFormData().getPtphWitnesses().get(0).getPoliceOfficerSubject().getOfficerInCharge(), is(true));
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseExists_DefendantData_In_Event() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet_defendantData_only.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }

    @Test
    public void shouldReturnReceivedCpsServeBcmProcessed_WhenProsecutionCaseExists_DefendantData_In_Event() {
        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject bcmDefendants = readJson("json/bcmDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServeBcmJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-defendantDataOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefileBcm.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataBcm(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(bcmDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveBcm(processReceivedCpsServeBcmJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null);

        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServeBcmProcessed) eventStream.findFirst().get();
        assertOnReceivedCpsServeBcmProcessed(caseId, receivedCpsServeBcmProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePtphProcessed_WhenProsecutionCaseExists_DefendantData_In_Event() {
        final JsonObject ptphFormData = readJson("json/ptphFormData.json", JsonObject.class);
        final JsonObject ptphDefendants = readJson("json/ptphDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePtphJson = readJson("json/public.stagingprosecutors.cps-serve-ptph-defendantDataOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefilePtph.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataPtph(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(ptphDefendants)
                .withFormData(ptphFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePtph(processReceivedCpsServePtphJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                null,
                Optional.empty(),
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter);

        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = (ReceivedCpsServePtphProcessed) eventStream.findFirst().get();
        assertOnReceivedCpsServePtphProcessed(caseId, receivedCpsServePtphProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseExists_OrganisationData_In_Event() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet_organisationData_only.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }

    @Test
    public void shouldReturnReceivedCpsServeBcmProcessed_WhenProsecutionCaseExists_OrganisationData_In_Event() {
        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject bcmDefendants = readJson("json/bcmDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServeBcmJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-receivedOrganisationDataOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefileBcm.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataBcm(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(bcmDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveBcm(processReceivedCpsServeBcmJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null);

        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServeBcmProcessed) eventStream.findFirst().get();
        assertOnReceivedCpsServeBcmProcessed(caseId, receivedCpsServeBcmProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePtphProcessed_WhenProsecutionCaseExists_OrganisationData_In_Event() {
        final JsonObject ptphFormData = readJson("json/ptphFormData.json", JsonObject.class);
        final JsonObject ptphDefendants = readJson("json/ptphDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePtphJson = readJson("json/public.stagingprosecutors.cps-serve-ptph-receivedOrganisationDataOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefilePtph.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataPtph(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(ptphDefendants)
                .withFormData(ptphFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePtph(processReceivedCpsServePtphJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                null,
                Optional.empty(),
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter);

        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = (ReceivedCpsServePtphProcessed) eventStream.findFirst().get();
        assertOnReceivedCpsServePtphProcessed(caseId, receivedCpsServePtphProcessed);
    }


    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseExists_ProsecutorDefendantId_In_Event() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet_prosecutorDefendantId_only.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }

    @Test
    public void shouldReturnReceivedCpsServeBcmProcessed_WhenProsecutionCaseExists_ProsecutorDefendantId_In_Event() {
        final JsonObject bcmFormData = readJson("json/bcmFormData.json", JsonObject.class);
        final JsonObject bcmDefendants = readJson("json/bcmDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServeBcmJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-prosecutorDefendantIdOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefileBcm.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataBcm(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(bcmDefendants)
                .withFormData(bcmFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveBcm(processReceivedCpsServeBcmJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null);

        final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed = (ReceivedCpsServeBcmProcessed) eventStream.findFirst().get();

        assertOnReceivedCpsServeBcmProcessed(caseId, receivedCpsServeBcmProcessed);
    }

    @Test
    public void shouldReturnReceivedCpsServePtphProcessed_WhenProsecutionCaseExists_ProsecutorDefendantId_In_Event() {
        final JsonObject ptphFormData = readJson("json/ptphFormData.json", JsonObject.class);
        final JsonObject ptphDefendants = readJson("json/ptphDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePtphJson = readJson("json/public.stagingprosecutors.cps-serve-bcm-prosecutorDefendantIdOnly.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefilePtph.case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormDataPtph(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(ptphDefendants)
                .withFormData(ptphFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePtph(processReceivedCpsServePtphJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                null,
                Optional.empty(),
                jsonObjectToObjectConverter,
                objectToJsonObjectConverter);

        final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed = (ReceivedCpsServePtphProcessed) eventStream.findFirst().get();

        assertOnReceivedCpsServePtphProcessed(caseId, receivedCpsServePtphProcessed);
    }

    private void assertOnReceivedCpsServeBcmProcessed(final UUID caseId, final ReceivedCpsServeBcmProcessed receivedCpsServeBcmProcessed) {
        assertThat(receivedCpsServeBcmProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServeBcmProcessed.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(receivedCpsServeBcmProcessed.getSubmissionId().toString(), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(receivedCpsServeBcmProcessed.getFormData(), notNullValue());
        final List<BcmDefendants> bcmDefendantsList = receivedCpsServeBcmProcessed.getFormData().getBcmDefendants();
        assertThat(receivedCpsServeBcmProcessed.getFormData().getBcmDefendants(), hasSize(1));
        BcmDefendants bcmDefendant = bcmDefendantsList.get(0);
        assertThat(bcmDefendant.getId().toString(), is("7f8fe782-a287-11eb-bcbc-0242ac130002"));
        assertThat(bcmDefendant.getAnyOther(), is("anyOther"));
        assertThat(bcmDefendant.getOtherAreasAfterPtph(), is("otherAreasAfterPtph"));
        assertThat(bcmDefendant.getOtherAreasBeforePtph(), is("otherAreasBeforePtph"));

        assertThat(bcmDefendant.getProsecutorOffences(), hasSize(1));
        ProsecutorOffences prosecutorOffence = bcmDefendant.getProsecutorOffences().get(0);
        assertThat(prosecutorOffence.getOffenceCode(), is("offenceCode"));
        assertThat(prosecutorOffence.getWording(), is("wording"));
        assertThat(prosecutorOffence.getDate(), is(convertToLocalDate("2022-05-11")));
    }

    private void assertOnReceivedCpsServePtphProcessed(final UUID caseId, final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed) {
        assertThat(receivedCpsServePtphProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePtphProcessed.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(receivedCpsServePtphProcessed.getSubmissionId().toString(), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(receivedCpsServePtphProcessed.getPtphFormData(), notNullValue());
        final List<PtphFormdefendants> ptphFormdefendants = receivedCpsServePtphProcessed.getPtphFormData().getPtphFormdefendants();
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphFormdefendants(), hasSize(1));
        PtphFormdefendants ptphDefendant = ptphFormdefendants.get(0);
        assertThat(ptphDefendant.getId(), is("7f8fe782-a287-11eb-bcbc-0242ac130002"));

    }

    private void assertOnReceivedCpsServePtphProcessedWithWitnessDetails(final UUID caseId, final ReceivedCpsServePtphProcessed receivedCpsServePtphProcessed) {
        assertThat(receivedCpsServePtphProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePtphProcessed.getSubmissionStatus(), is(SubmissionStatus.SUCCESS));
        assertThat(receivedCpsServePtphProcessed.getSubmissionId().toString(), is("e85d2c62-af1f-4674-863a-0891e67e325b"));
        assertThat(receivedCpsServePtphProcessed.getPtphFormData(), notNullValue());
        final List<PtphFormdefendants> ptphFormdefendants = receivedCpsServePtphProcessed.getPtphFormData().getPtphFormdefendants();
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphFormdefendants(), hasSize(1));
        PtphFormdefendants ptphDefendant = ptphFormdefendants.get(0);
        assertThat(ptphDefendant.getId(), is("7f8fe782-a287-11eb-bcbc-0242ac130002"));
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphWitnesses().size(), is(1));
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphWitnesses().get(0), notNullValue());
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphWitnesses().get(0).getPoliceOfficerSubject(), notNullValue());
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphWitnesses().get(0).getPoliceOfficerSubject().getOfficeCollarNumber(), is("45"));
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphWitnesses().get(0).getPoliceOfficerSubject().getOfficerRank(), is("Constable"));
        assertThat(receivedCpsServePtphProcessed.getPtphFormData().getPtphWitnesses().get(0).getPoliceOfficerSubject().getOfficerInCharge(), is(true));

    }

    @Test
    public void shouldReturnReceivedCpsServeCotrProcessed_WhenProsecutionCaseExists_ThereIsNoValidationError() {
        final JsonObject cotrDefendants = readJson("json/bcmDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServeCotrJson = readJson("json/process-received-cps-serve-cotr.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefileBcm.case.json", JsonObject.class);

        when(cpsFormValidator.validateCotr(any(), any(), any())).thenReturn(formValidationResult()
                .withFormDefendants(cotrDefendants)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceiveCotr(processReceivedCpsServeCotrJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                null);

        final ReceivedCpsServeCotrProcessed receivedCpsServeCotrProcessed = (ReceivedCpsServeCotrProcessed) eventStream.findFirst().get();
        assertThat(receivedCpsServeCotrProcessed.getCaseId(), is(caseId));
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessedWithdefendantInformationUpdated() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case-with-address.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();
        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants().get(0).getAddressAndContact().getAddress(), is("66 Exeter Street,line 2,line 3,line 4,line 5,M60 1NW"));
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessedWithOrgdefendantInformationUpdated() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case-without-personalInfo.json", JsonObject.class);
        final JsonObject progressionCaseFileJson = readJson("json/progression-prosecution-case.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());
        final UUID caseId = randomUUID();
        when(progressionService.getProsecutionCase(any())).thenReturn(progressionCaseFileJson);
        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants().get(0).getAddressAndContact().getAddress(), is("jotTSWovy5,56Police House,StreetDescription3,Locality3,Town6,TW14 9XD"));
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessedWithTrialRepresentative() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case-with-address.json", JsonObject.class);
        final JsonObject trialRepresentativeJson = readJson("json/associated-defence-organisation.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());

        final UUID caseId = randomUUID();
        final UUID defendantId = UUID.fromString(petFormData.getJsonObject("defence").getJsonArray("defendants").getJsonObject(0).getString("id"));

        when(defenceService.getAssociatedOrganisation(defendantId)).thenReturn(trialRepresentativeJson);

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter, new ProsecutionCaseFile(), progressionService, defenceService);

        final Optional<Object> event = eventStream.findFirst();

        if(event.isPresent()) {
            final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) event.get();

            assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
            final DefendantTrialRepresentative trialRepresentative = receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants().get(0).getTrialRepresentative();
            assertNotNull(trialRepresentative);
            assertThat(trialRepresentative.getRepresentative(), is("Jedi and Sons LLP"));
            assertThat(trialRepresentative.getRepresentativeAddress(), is("Jedi and Sons LLP, 35 Bridget Avenue, London, England, CR5 7UH"));
            assertThat(trialRepresentative.getEmail(), is("jedi@jedi.test.com"));
            assertThat(trialRepresentative.getPhone(), is("01234555666"));
        }
        else {
            fail();
        }
    }
    @Test
    public void shouldReturnReceivedCpsServePetProcessedWithNoTrialRepresentativeFromDefence() {
        final JsonObject petFormData = readJson("json/petFormData.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = readJson("json/prosecutioncasefile.case-with-address.json", JsonObject.class);
        final JsonObject trialRepresentativeJson = readJson("json/associated-defence-organisation-empty.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(SubmissionStatus.SUCCESS)
                .build());

        final UUID caseId = randomUUID();

        when(defenceService.getAssociatedOrganisation(any())).thenReturn(trialRepresentativeJson);

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.of(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter, new ProsecutionCaseFile(), progressionService, defenceService);

        final Optional<Object> event = eventStream.findFirst();

        if(event.isPresent()) {
            final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) event.get();

            assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
            final DefendantTrialRepresentative trialRepresentative = receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants().get(0).getTrialRepresentative();
            assertNotNull(trialRepresentative);
            assertNull(trialRepresentative.getRepresentative());
            assertNull(trialRepresentative.getRepresentativeAddress());
            assertNull(trialRepresentative.getEmail());
            assertNull(trialRepresentative.getPhone());
        }
        else {
            fail();
        }
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_CpsDefendantId_Success_Event() {
        final UUID caseId = randomUUID();
        createFormValidationResult(SubmissionStatus.SUCCESS);

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(
                readJson("json/process-received-cps-serve-pet_cpsDefendantId_only.json", JsonObject.class),
                "SUCCESS",
                caseId,
                Optional.of(readJson("json/prosecutioncasefile.case.json", JsonObject.class)),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_CpsDefendantId_Rejected_Event() {
        final UUID caseId = randomUUID();
        createFormValidationResult(SubmissionStatus.REJECTED);

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(
                readJson("json/process-received-cps-serve-pet_cpsDefendantId_only.json", JsonObject.class),
                "SUCCESS",
                caseId,
                Optional.of(readJson("json/prosecutioncasefile.case.json", JsonObject.class)),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));
        assertThat(receivedCpsServePetProcessed.getPetFormData().getDefence().getDefendants(), hasSize(1));
    }

    private void createFormValidationResult(final SubmissionStatus submissionStatus){
        final JsonObject petFormData = readJson("json/petFormData1.json", JsonObject.class);
        final JsonObject petDefendants = readJson("json/petDefendants.json", JsonObject.class);

        when(cpsFormValidator.validateAndRebuildingFormData(any(), any(), any(), any())).thenReturn(formValidationResult()
                .withPetDefendants(petDefendants)
                .withPetFormData(petFormData)
                .withSubmissionStatus(submissionStatus)
                .build());
    }

    @Test
    public void shouldReturnReceivedCpsServePetProcessed_WhenProsecutionCaseDoesNotExist_WhenWitnessAgeAndMeasuresReqdDoesNotExist() {
        final JsonObject processReceivedCpsServePetJson = readJson("json/process-received-cps-serve-pet-withoutAgeAndMeasuresReqd.json", JsonObject.class);
        final JsonObject prosecutionCaseFileJson = null;

        final UUID caseId = randomUUID();

        final Stream<Object> eventStream = cpsServeMaterialAggregate.cpsReceivePet(processReceivedCpsServePetJson,
                "SUCCESS",
                caseId,
                Optional.ofNullable(prosecutionCaseFileJson),
                cpsFormValidator,
                validOffences,
                null,
                this.jsonObjectToObjectConverter,new ProsecutionCaseFile(), progressionService, defenceService);

        final ReceivedCpsServePetProcessed receivedCpsServePetProcessed = (ReceivedCpsServePetProcessed) eventStream.findFirst().get();

        assertThat(receivedCpsServePetProcessed.getCaseId(), is(caseId));

        final Witnesses witnesses = receivedCpsServePetProcessed.getPetFormData().getProsecution().getWitnesses().get(0);
        assertNull(witnesses.getAge());
        assertTrue(witnesses.getMeasuresRequired().isEmpty());
    }
}