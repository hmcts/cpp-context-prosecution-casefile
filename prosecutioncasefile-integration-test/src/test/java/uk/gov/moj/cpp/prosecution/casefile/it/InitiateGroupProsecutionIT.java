package uk.gov.moj.cpp.prosecution.casefile.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateGroupProsecutionHelper;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import uk.gov.justice.services.messaging.JsonEnvelope;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
        final String payload = replaceValues(staticPayLoad, "S"); //"S" = SUMMONS
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        initiateGroupProsecutionHelper.thenPrivateGroupCasesParkedForApprovalEventShouldBeRaised();
        initiateGroupProsecutionHelper.thenPrivateGroupIdRecorderdForSummonsApplicationEventShouldBeRaised();
        initiateGroupProsecutionHelper.verifyInitiateCourtProceedingsForApplicationCommand();
        initiateGroupProsecutionHelper.verifyCreateDocumentCalled(asList("dateReceived"));
        initiateGroupProsecutionHelper.verifyUploadMaterialCommandCalled();
        initiateGroupProsecutionHelper.verifyAddCourtDocumentCalled(caseId1.toString());

        final JsonEnvelope parkedForSummonsApplicationApprovalEvent = initiateGroupProsecutionHelper.thenPublicGroupParkedForSummonsApplicationApprovalEventShouldBeRaised();
        assertThat(parkedForSummonsApplicationApprovalEvent.payloadAsJsonObject().getString("channel"), is("CIVIL"));
        assertThat(parkedForSummonsApplicationApprovalEvent.payloadAsJsonObject().getString("groupId"), is(groupId.toString()));

        final UUID applicationId = randomUUID();
        stubForQueryApplication(applicationId);
        sendPublicEventCourtApplicationSummonsApproved(caseId1, applicationId);
        initiateGroupProsecutionHelper.thenPrivateGroupCasesReceivedEventShouldBeRaised();
        initiateGroupProsecutionHelper.verifyInitiateCourtProceedingsForGroupCasesCommand(caseId1.toString());

        // group-submission-approved fires as soon as GroupCasesReceived is raised (i.e. as soon as
        // the box approves the summons application) — it does not wait for Progression's later
        // group-prosecution-cases-created confirmation.
        final JsonEnvelope submissionApprovedEvent = initiateGroupProsecutionHelper.thenPublicGroupSubmissionApprovedEventShouldBeRaised();
        assertThat(submissionApprovedEvent.payloadAsJsonObject().getString("groupId"), is(groupId.toString()));
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
        initiateGroupProsecutionHelper.thenPublicGroupParkedForSummonsApplicationApprovalEventShouldBeRaised();

        sendPublicEventCourtApplicationSummonsRejected(caseId1, randomUUID());
        initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();

        final JsonEnvelope submissionRejectedEvent = initiateGroupProsecutionHelper.thenPublicGroupSubmissionRejectedEventShouldBeRaised();
        assertThat(submissionRejectedEvent.payloadAsJsonObject().getString("groupId"), is(groupId.toString()));
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
    void shouldRaiseGroupProsecutionRejectedForCivilCaseWithMoreThanOneDefendant() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-civil-group-prosecution-with-multiple-defendants.json");
        final String payload = replaceValues(staticPayLoad, "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        final JsonEnvelope rejectedEvent = initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();
        assertThat(rejectedEvent.payloadAsJsonObject().get("groupCaseErrors").toString(),
                containsString("MORE_THAN_ONE_DEFENDANT_PER_PROSECUTION_CASE"));
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

    @Test
    void shouldRaiseGroupProsecutionRejectedWhenCivilGroupCaseHasPastHearingDate() {
        final String payload = replaceValues(readFile("command-json/prosecutioncasefile.command.initiate-civil-group-prosecution-past-hearing-date.json"), "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        final JsonEnvelope rejectedEvent = initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();
        assertThat(rejectedEvent.payloadAsJsonObject().get("groupCaseErrors").toString(), containsString("DATE_OF_HEARING_IN_THE_PAST"));
    }

    /**
     * caseErrors on group-prosecution-rejected is now "one case-problem.json per case that failed
     * case-level validation", each tagged with THAT case's own prosecutorCaseReference, rather than
     * one flat merged list of problem.json objects for the whole submission.
     *
     * The fixture puts a distinct INVALID initiationCode on case 2 ("Z") and case 3 ("Y") while the
     * master case 1 keeps a valid one, so the assertion can prove three things at once:
     * only the failing cases appear, each is tagged with its own reference, and each carries only
     * its own problem values (no cross-case merging).
     */
    @Test
    void shouldTagEachCasesProblemsWithThatCasesOwnProsecutorCaseReferenceOnGroupProsecutionRejected() {
        final String payload = replaceValues(readFile("command-json/prosecutioncasefile.command.initiate-civil-group-prosecution-with-case-level-errors-on-multiple-cases.json"), "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        final JsonEnvelope rejectedEvent = initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();

        final JsonArray caseErrors = rejectedEvent.payloadAsJsonObject().getJsonArray("caseErrors");
        assertThat("expected one case-problem per failing case, got " + caseErrors, caseErrors.size(), is(2));

        final Map<String, JsonArray> problemsByCaseReference = new HashMap<>();
        for (final JsonValue caseError : caseErrors) {
            final JsonObject caseProblem = (JsonObject) caseError;
            problemsByCaseReference.put(caseProblem.getString("prosecutorCaseReference"), caseProblem.getJsonArray("problems"));
        }

        assertThat(problemsByCaseReference.keySet(), containsInAnyOrder(caseUrn2, caseUrn3));
        assertThat("the valid master case must not appear in caseErrors", problemsByCaseReference.containsKey(caseUrn1), is(false));

        assertCaseHasOnlyItsOwnInitiationCodeProblem(problemsByCaseReference.get(caseUrn2), "Z");
        assertCaseHasOnlyItsOwnInitiationCodeProblem(problemsByCaseReference.get(caseUrn3), "Y");
    }

    private void assertCaseHasOnlyItsOwnInitiationCodeProblem(final JsonArray problems, final String expectedInitiationCode) {
        assertThat("expected exactly one problem, got " + problems, problems.size(), is(1));
        final JsonObject problem = problems.getJsonObject(0);
        assertThat(problem.getString("code"), is("CASE_INITIATION_CODE_INVALID"));
        assertThat(problem.getJsonArray("values").getJsonObject(0).getString("value"), is(expectedInitiationCode));
    }

    /**
     * Covers the PENDING -> FAILED status transition for a civil group submission rejected for
     * OFFENCE_CODE_NOT_SUPPORTED (an offence code reference data doesn't recognise) — CPCI's
     * business-validation-failure scenario. Deliberately a single defendant/single case so only
     * the offence-code rule fires (not the unrelated more-than-one-defendant group rule).
     */
    @Test
    void shouldRaiseGroupProsecutionRejectedWhenOffenceCodeIsNotSupported() {
        final String payload = replaceValues(readFile("command-json/prosecutioncasefile.command.initiate-civil-group-prosecution-invalid-offence-code.json"), "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        final JsonEnvelope rejectedEvent = initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();
        assertThat(rejectedEvent.payloadAsJsonObject().getString("channel"), is("CIVIL"));
        // OFFENCE_CODE_NOT_SUPPORTED is a defendant/offence-level problem, so it is carried on
        // defendantErrors rather than caseErrors (which is reserved for case-level validation rules).
        assertThat(rejectedEvent.payloadAsJsonObject().get("defendantErrors").toString(), containsString("OFFENCE_CODE_NOT_SUPPORTED"));
    }

    @Test
    void shouldRaiseGroupProsecutionRejectedWhenCivilGroupCaseHasDuplicateUrn() {
        final String payload = replaceValues(readFile("command-json/prosecutioncasefile.command.initiate-civil-group-prosecution-duplicate-urn.json"), "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        final JsonEnvelope rejectedEvent = initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();
        assertThat(rejectedEvent.payloadAsJsonObject().get("groupCaseErrors").toString(), containsString("DUPLICATED_PROSECUTION"));
    }

    /**
     * groupCaseErrors on group-prosecution-rejected is now a list of case-problem.json entries (the same
     * wrapper shape as caseErrors) rather than a flat list of problem.json entries.
     *
     * Group-level problems belong to the submission as a whole, not to one case, so all of them are wrapped
     * in a single case-problem whose prosecutorCaseReference is null. The framework's ObjectMapper serialises
     * with NON_ABSENT inclusion, so a null prosecutorCaseReference means the key is omitted from the published
     * JSON altogether — hence the assertion that the key is simply not there.
     *
     * The duplicate-URN fixture puts the same prosecutorCaseReference on both cases in the group, which trips
     * the group-level DuplicateProsecutionReferenceValidationRule while both cases pass case-level validation.
     */
    @Test
    void shouldWrapGroupLevelProblemsInOneCaseProblemWithNoProsecutorCaseReferenceOnGroupProsecutionRejected() {
        final String payload = replaceValues(readFile("command-json/prosecutioncasefile.command.initiate-civil-group-prosecution-duplicate-urn.json"), "O");
        final InitiateGroupProsecutionHelper initiateGroupProsecutionHelper = new InitiateGroupProsecutionHelper();
        initiateGroupProsecutionHelper.initiateGroupProsecution(payload);
        final JsonEnvelope rejectedEvent = initiateGroupProsecutionHelper.thenPublicGroupProsecutionRejectedEventShouldBeRaised();

        final JsonArray groupCaseErrors = rejectedEvent.payloadAsJsonObject().getJsonArray("groupCaseErrors");
        assertThat("all group-level problems must be wrapped in exactly one case-problem, got " + groupCaseErrors,
                groupCaseErrors.size(), is(1));

        final JsonObject groupCaseError = groupCaseErrors.getJsonObject(0);
        assertThat("a group-level problem is not attributable to a single case, so prosecutorCaseReference must not be published: " + groupCaseError,
                groupCaseError.containsKey("prosecutorCaseReference"), is(false));
        assertThat("case-problem wrapper must carry only problems for group-level errors: " + groupCaseError,
                groupCaseError.keySet(), contains("problems"));

        final JsonArray problems = groupCaseError.getJsonArray("problems");
        assertThat("expected exactly one group-level problem, got " + problems, problems.size(), is(1));

        final JsonObject duplicateUrnProblem = problems.getJsonObject(0);
        assertThat(duplicateUrnProblem.getString("code"), is("DUPLICATED_PROSECUTION"));

        final JsonArray values = duplicateUrnProblem.getJsonArray("values");
        assertThat("expected the duplicated urn to be reported once, got " + values, values.size(), is(1));
        assertThat(values.getJsonObject(0).getString("key"), is(caseUrn1));
        assertThat(values.getJsonObject(0).getString("value"), is("2"));
    }

    // initiationCode "O" = OTHER (civil group case path), "S" = SUMMONS (summons application path)
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
