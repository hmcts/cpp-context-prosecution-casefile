package uk.gov.moj.cpp.prosecution.casefile.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateGroupProsecutionHelper;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.helper.WiremockTestHelper.createCommonMockEndpoints;
import static uk.gov.moj.cpp.prosecution.casefile.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.prosecution.casefile.stub.MaterialStub.stubForUploadFileCommand;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForAddCourtDocument;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForInitiateCourtProceedings;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForInitiateCourtProceedingsForApplication;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForQueryApplication;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeForGroupCases;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubApplicationTypes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCustodyStatuses;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetHearingTypes;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetObservedEthnicities;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnits;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetSelfDefinedEthnicities;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

public class InitiateGroupProsecutionIT extends BaseIT {
    private static final String CASE_MARKER_CODE = "ABC";
    private static final String PUBLIC_COURT_APPLICATION_SUMMONS_APPROVED = "public.progression.court-application-summons-approved";
    private static final String PUBLIC_COURT_APPLICATION_SUMMONS_REJECTED = "public.progression.court-application-summons-rejected";

    private UUID groupId;
    private UUID externalId;
    private UUID caseId1;
    private UUID caseId2;
    private UUID caseId3;
    private String caseUrn1;
    private String caseUrn2;
    private String caseUrn3;
    private String defendantId1;
    private String defendantId2;
    private String defendantId3;
    private String offenceId1;
    private String offenceId2;
    private String offenceId3;

    @BeforeAll
    public static void setUpOnce() {
        createCommonMockEndpoints();
        stubWiremocks();
    }

    @BeforeEach
    public void setUp() {
        groupId = randomUUID();
        caseId1 = randomUUID();
        caseId2 = randomUUID();
        caseId3 = randomUUID();
        caseUrn1 = randomAlphanumeric(10);
        caseUrn2 = randomAlphanumeric(10);
        caseUrn3 = randomAlphanumeric(10);
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        defendantId3 = randomUUID().toString();
        offenceId1 = randomUUID().toString();
        offenceId2 = randomUUID().toString();
        offenceId3 = randomUUID().toString();
        externalId = randomUUID();
    }


    private static void stubWiremocks() {
        stubApplicationTypes();
        stubGetOrganisationUnits();
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
        stubGetSelfDefinedEthnicities();
        stubGetObservedEthnicities();
        stubForInitiateCourtProceedings();
        stubGetHearingTypes();
        stubGetCustodyStatuses();
        stubOffencesForOffenceCodeForGroupCases();
        stubForInitiateCourtProceedingsForApplication();
        stubDocumentCreate("PDF Document");
        stubForUploadFileCommand();
        stubForAddCourtDocument();
    }

    @Test
    public void shouldInitiateCourtProceedingsForApplication() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-group-prosecution.json");
        final String payload = replaceValues(staticPayLoad, "S");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        initiateGroupProsecutionHelper.thenPrivateGroupCasesParkedForApprovalEventShouldBeRaised();
        initiateGroupProsecutionHelper.thenPrivateGroupIdRecorderdForSummonsApplicationEventShouldBeRaised();
        initiateGroupProsecutionHelper.verifyInitiateCourtProceedingsForApplicationCommand();
        initiateGroupProsecutionHelper.verifyCreateDocumentCalled(asList("dateReceived"));
        initiateGroupProsecutionHelper.verifyUploadMaterialCommandCalled();
        initiateGroupProsecutionHelper.verifyAddCourtDocumentCalled(caseId1.toString());

        final UUID applicationId = randomUUID();
        stubForQueryApplication(applicationId);
        sendPublicEventCourtApplicationSummonsApproved(caseId1, applicationId);
        initiateGroupProsecutionHelper.thenPrivateGroupCasesReceivedEventShouldBeRaised();
        initiateGroupProsecutionHelper.verifyInitiateCourtProceedingsForGroupCasesCommand(caseId1.toString());
    }


    @Test
    public void shouldRejectCourtProceedingsForApplication() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-group-prosecution.json");
        final String payload = replaceValues(staticPayLoad, "S");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        initiateGroupProsecutionHelper.thenPrivateGroupCasesParkedForApprovalEventShouldBeRaised();
        initiateGroupProsecutionHelper.thenPrivateGroupIdRecorderdForSummonsApplicationEventShouldBeRaised();
        initiateGroupProsecutionHelper.verifyInitiateCourtProceedingsForApplicationCommand();
        initiateGroupProsecutionHelper.verifyCreateDocumentCalled(asList("dateReceived"));
        initiateGroupProsecutionHelper.verifyUploadMaterialCommandCalled();
        initiateGroupProsecutionHelper.verifyAddCourtDocumentCalled(caseId1.toString());

        sendPublicEventCourtApplicationSummonsRejected(caseId1, randomUUID());
        initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();
    }

    @Test
    public void shouldInitiateCourtProceedingsForGroupCases() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-group-prosecution.json");
        final String payload = replaceValues(staticPayLoad, "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        initiateGroupProsecutionHelper.thenPrivateGroupCasesReceivedEventShouldBeRaised();
        initiateGroupProsecutionHelper.verifyInitiateCourtProceedingsForGroupCasesCommand(caseId1.toString());
    }

    @Test
    void shouldInitiateCourtProceedingsForGroupCasesForMandatoryValuesOnly() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-group-prosecution-mandatory-values-only.json");
        final String payload = replaceValues(staticPayLoad, "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        initiateGroupProsecutionHelper.thenPrivateGroupCasesReceivedEventShouldBeRaised();
        initiateGroupProsecutionHelper.verifyInitiateCourtProceedingsForGroupCasesCommand(caseId1.toString());
    }

    @Test
    public void shouldRaiseGroupProsecutionRejectedWhenValidationFails() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-group-prosecution-with-one-case.json");
        final String payload = replaceValues(staticPayLoad, "S");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();
    }

    private void sendPublicEventCourtApplicationSummonsApproved(final UUID caseId, final UUID applicationId) {
        final JsonObject payload = createObjectBuilder()
                .add("id", applicationId.toString())
                .add("prosecutionCaseId", caseId.toString())
                .add("summonsApprovedOutcome", createObjectBuilder()
                        .add("prosecutorCost", "1")
                        .add("prosecutorEmailAddress", "a")
                        .add("summonsSuppressed", false)
                        .add("personalService", true))
                .build();

        sendPublicEvent(PUBLIC_COURT_APPLICATION_SUMMONS_APPROVED, envelopeFrom(metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_COURT_APPLICATION_SUMMONS_APPROVED)
                .withUserId(randomUUID().toString())
                .build(), payload));
    }

    private void sendPublicEventCourtApplicationSummonsRejected(final UUID caseId, final UUID applicationId) {
        final JsonObject payload = createObjectBuilder()
                .add("id", applicationId.toString())
                .add("prosecutionCaseId", caseId.toString())
                .add("summonsRejectedOutcome", createObjectBuilder()
                        .add("prosecutorEmailAddress", "a")
                        .add("reasons", createArrayBuilder().add("any reason")))
                .build();

        sendPublicEvent(PUBLIC_COURT_APPLICATION_SUMMONS_REJECTED, envelopeFrom(metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_COURT_APPLICATION_SUMMONS_REJECTED)
                .withUserId(randomUUID().toString())
                .build(), payload));
    }

    private String replaceValues(final String payload, final String initiationCode) {
        return payload
                .replace("CASE_ID_1", this.caseId1.toString())
                .replace("CASE_ID_2", this.caseId2.toString())
                .replace("CASE_ID_3", this.caseId3.toString())
                .replace("CASE_URN_1", caseUrn1)
                .replace("CASE_URN_2", caseUrn2)
                .replace("CASE_URN_3", caseUrn3)
                .replace("DEFENDANT_ID_1", this.defendantId1)
                .replace("DEFENDANT_ID_2", this.defendantId2)
                .replace("DEFENDANT_ID_3", this.defendantId3)
                .replace("DEFENDANT_REFERENCE_1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE_2", this.defendantId2)
                .replace("DEFENDANT_REFERENCE_3", this.defendantId3)
                .replace("OFFENCE_ID_1", this.offenceId1)
                .replace("OFFENCE_ID_2", this.offenceId2)
                .replace("OFFENCE_ID_3", this.offenceId3)
                .replaceAll("GROUP_ID", groupId.toString())
                .replaceAll("INITIATION_CODE", initiationCode)
                .replaceAll("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replaceAll("EXTERNAL_ID", this.externalId.toString());
    }
}
