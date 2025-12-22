package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.helper.DefaultRequests.getCaseDetailsByProsecutionReferenceIdBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForMojOffenceCodeList;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeWithEitherWayModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeWithSummaryOnlyModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetLocalJusticeAreas;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroom;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroomForCrown;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitWithOneCourtroomForMags;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitsReturnsMag;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetPoliceForces;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateCCProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.helper.SummonsHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class InitiateCCProsecutionIT extends BaseIT {

    private static final String ADD_DEFENDANT_TO_COURT_PROCEEDING = "/progression-service/command/api/rest/progression/adddefendantstocourtproceedings";

    private static final String CASE_MARKER_CODE = "ABC";
    private static final String EITHER_WAY_MOT_REASON_ID = "78efce20-8a52-3272-9d22-2e7e6e3e565e";
    public static final String CIVIL_CJS_OFFENCE_CODE = "DA21503";

    private UUID caseId;
    private String caseUrn;
    private UUID externalId;
    private String defendantId1;
    private String defendantId2;
    private String defendantId3;
    private String offenceId1;
    private String offenceId2;
    private String offenceId3;
    private String offenceId4;
    private String offenceId5;
    private String offenceId6;

    @BeforeAll
    public static void setup() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
        stubGetOrganisationUnitsReturnsMag("B04NM04");
        stubGetOrganisationUnitsReturnsMag("B02NM03");
    }

    @BeforeEach
    void setUp() {
        stubOffencesForOffenceCodeWithEitherWayModeOfTrial();
        caseId = randomUUID();
        externalId = randomUUID();
        caseUrn = randomAlphanumeric(10);
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        defendantId3 = randomUUID().toString();
        offenceId1 = randomUUID().toString();
        offenceId2 = randomUUID().toString();
        offenceId3 = randomUUID().toString();
        offenceId4 = randomUUID().toString();
        offenceId5 = randomUUID().toString();
        offenceId6 = randomUUID().toString();
        externalId = randomUUID();
    }

    @Test
    void initiateCCProsecution() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output.json");
        final String ccPayLoad = replaceValues(staticPayLoad);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised();
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, expectedPayload);
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED});
    }

    @Test
    void initiateCCProsecutionForMCC() {
        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-mcc.json");
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void initiateCCProsecutionForMCCWithRetrial() {
        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-retrial.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-mcc-retrial.json");
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    public void initiateCCProsecutionOfCivilCaseWithCivilFeeViaMCCJourney() {
        final UUID offenceId = randomUUID();
        stubGetPoliceForces();
        stubGetOrganisationUnitWithOneCourtroom();
        stubOffencesForMojOffenceCodeList(CIVIL_CJS_OFFENCE_CODE, offenceId.toString(), "MoJ");
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.initiate-cc-prosecution-with-mcc-civil-case-with civil-fees.json")
                .replaceAll("INITIATION_CODE", "O")
                .replaceAll("OFFENCE_CODE", CIVIL_CJS_OFFENCE_CODE)
                .replaceAll("OFFENCE_ID", offenceId.toString());
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        //Below data is coming from above input file.
        String startDateTime = dateFormat.format(LocalDate.now().plusMonths(2).atTime(11,11,00));
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-civilcase-with-civil-fees-via-mcc-journey.json")
                .replaceAll("LISTED_START_DATE_TIME", startDateTime)
                .replaceAll("OFFENCE_CODE", CIVIL_CJS_OFFENCE_CODE)
                .replaceAll("OFFENCE_ID", offenceId.toString());
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    public void initiateCCProsecutionOfCivilCaseWithCivilFeeViaMCCJourneyOne() {
        final UUID offenceId = randomUUID();
        stubGetPoliceForces();
        stubGetOrganisationUnitWithOneCourtroom();
        stubOffencesForMojOffenceCodeList(CIVIL_CJS_OFFENCE_CODE, offenceId.toString(), "MoJ");
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.initiate-cc-prosecution-with-mcc-civil-case-with civil-fees.json")
                .replaceAll("INITIATION_CODE", "O")
                .replaceAll("OFFENCE_CODE", CIVIL_CJS_OFFENCE_CODE)
                .replaceAll("OFFENCE_ID", offenceId.toString());
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        //Below data is coming from above input file.
        String startDateTime = dateFormat.format(LocalDate.now().plusMonths(2).atTime(11,11,00));
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-civilcase-with-civil-fees-via-mcc-journey.json")
                .replaceAll("LISTED_START_DATE_TIME", startDateTime)
                .replaceAll("OFFENCE_CODE", CIVIL_CJS_OFFENCE_CODE)
                .replaceAll("OFFENCE_ID", offenceId.toString());
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void initiateCCProsecutionForMCCWhenOuCodeIsNull() {

        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-without-oucode.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-mcc-without-oucode.json");
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void initiateNonStandardProsecutionForMCC() {

        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-non-standard-prosecution-mcc.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-mcc-with-nsp.json");
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }


    @Test
    void initiateCCProsecutionForMCCAndTrial() {

        stubGetOrganisationUnitWithOneCourtroomForMags();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-trial.json");
        final String expectedPayload = replaceValues(readFile("expected/initiate_cc_expected_output-mcc-trial.json"));
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void initiateCCProsecutionForMCCAndWCTrialBadRequest() {

        stubGetOrganisationUnitWithOneCourtroomForMags();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-wc-bad-trial.json");
        verifyBadRequestPayload(staticPayLoad);
    }

    @Test
    void initiateCCProsecutionForMCCAndFIXEDEarliestDateBadRequest() {

        stubGetOrganisationUnitWithOneCourtroomForCrown();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-fixed-earliest-date.json");
        verifyBadRequestPayload(staticPayLoad);
    }

    @Test
    void setAllocationDecisionForSummaryOnlyOffenceForMCCAndTrialOrCommittalForSentence() {

        stubGetOrganisationUnitWithOneCourtroomForMags();
        stubOffencesForOffenceCodeWithSummaryOnlyModeOfTrial();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-sentence-summary-only-offence.json");
        final String expectedPayload = replaceValues(readFile("expected/initiate_cc_expected_output-mcc-summary-only-offence.json"));
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void shouldSetAllocationDecisionForMCCAndTrialOrCommittalForSentenceWhenMotReasonIdExists() {

        stubGetOrganisationUnitWithOneCourtroomForMags();
        stubOffencesForOffenceCodeWithEitherWayModeOfTrial();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-sentence-with-mot-reason-id.json")
                .replace("MOT_REASON_ID", EITHER_WAY_MOT_REASON_ID);
        final String expectedPayload = replaceValues(readFile("expected/initiate_cc_expected_output-mcc-with-allocation-decision-is-either-way.json"));
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void initiateCCProsecutionForMCCAndTrialWithGuiltyPleaToSetConvictionDate() {

        stubGetOrganisationUnitWithOneCourtroomForMags();
        stubGetLocalJusticeAreas();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-trial-guilty-plea.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-mcc-trial-guilty-plea.json");
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void shouldFailWithBadRequestForinvalidNumericLibraRefNumber() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-defendant-libra-ref-invalid-integer.json");
        verifyBadRequestPayload(staticPayLoad);
    }

    @Test
    void shouldFailWithBadRequestForinvalidAlphaNumericLibraRefNumber() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-defendant-libra-ref-invalid-alphanumeric.json");
        verifyBadRequestPayload(staticPayLoad);
    }


    @Test
    void initiateCCProsecutionForMCCAndTrialWithIndicatedGuiltyPleaToSetConvictionDate() {
        stubGetOrganisationUnitWithOneCourtroomForMags();
        stubGetLocalJusticeAreas();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-mcc-trial-indicated-guilty-plea.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output-mcc-trial-indicated-guilty-plea.json");
        verifyCCEventAndProgressionCommandForMCC(staticPayLoad, expectedPayload);
    }

    @Test
    void initiateCCProsecutionForPostalRequisitionWithCorporateDefendant() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-requisition-corporate.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output_requisition_corporate.json");
        final String expectedPublicEventCCPayload = readFile("expected/expected_cc_corporate_public_event.json");

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final String ccPayLoad = replaceValues(staticPayLoad);
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised();
        final Optional<JsonEnvelope> jsonEnvelope = initiateCCProsecutionHelper.retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED);
        assertThat(jsonEnvelope.isPresent(), is(true));
        assertPublicEventPayload(jsonEnvelope.get().payload().toString(), expectedPublicEventCCPayload);
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, expectedPayload);
    }

    @Test
    void initiateCCProsecutionForPostalRequisitionWithAllFields() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-requisition-all-fields.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output_requisition_all_fields.json");
        final String expectedPublicEventCCPayload = readFile("expected/expected_cc_requisition_all_fields_public_event.json");
        verifyCCEventAndProgressionCommand(staticPayLoad, expectedPayload, EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED, of(expectedPublicEventCCPayload));
    }

    @Test
    void initiateCCProsecutionForPostalRequisitionWithMandatoryFields() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-requisition-mandatory-fields.json");
        final String expectedPayload = readFile("expected/initiate_cc_expected_output_requisition_mandatory_fields.json");
        final String expectedPublicEventCCPayload = readFile("expected/expected_cc_requisition_mandatory_fields_public_event.json");
        verifyCCEventAndProgressionCommand(staticPayLoad, expectedPayload, EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED, of(expectedPublicEventCCPayload));
    }

    @Test
    void initiateCCProsecutionForCorporate() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-corporate.json");
        final String expectedPayload = readFile("expected/initiate_cc_corporate_expected_output.json");
        verifyCCEventAndProgressionCommand(staticPayLoad, expectedPayload, EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED, empty());
    }

    @Test
    void initiateCCProsecutionWithWarningsForCPPI() {
        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-offence-out-of-time.json");
        final String expectedProgressionPayload = readFile("expected/initiate_cc_offence_out_of_time.json");
        final String expectedCcWithWarningsPayload = readFile("expected/expected_cc_prosecution_received_with_warnings.json");
        verifyCCWithWarningsEventAndProgressionCommand(staticPayLoad, expectedProgressionPayload, EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS, expectedCcWithWarningsPayload);
    }

    @Test
    void initiateCCProsecutionWithWarningsForMCC() {
        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-offence-out-of-time-mcc.json");
        final String expectedProgressionPayload = readFile("expected/initiate_cc_offence_out_of_time-mcc.json");
        final String expectedCcWithWarningsPayload = readFile("expected/expected_cc_prosecution_received_with_warnings-mcc.json");
        verifyCCWithWarningsEventAndProgressionCommandForMCC(staticPayLoad, expectedProgressionPayload, expectedCcWithWarningsPayload);
    }

    @Test
    void initiateCCProsecutionDefendantOrganisationWithWarningsForMCC() {
        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-defendant-organisation-with-offence-out-of-time-mcc.json");
        final String expectedProgressionPayload = readFile("expected/initiate_cc_offence_out_of_time-mcc-with-defendant-organisation.json");
        final String expectedCcWithWarningsPayload = readFile("expected/expected_cc_prosecution_received_with_warnings-mcc-with-defendant-organisation.json");
        verifyCCWithWarningsEventAndProgressionCommandForMCC(staticPayLoad, expectedProgressionPayload, expectedCcWithWarningsPayload, true);
    }

    @Test
    void initiateCCProsecutionWithOffenceNotInEffectWarning() {
        stubGetOrganisationUnitWithOneCourtroom();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-offence-not-in-effect.json");
        final String expectedProgressionPayload = readFile("expected/initiate_cc_offence_not_in_effect.json");
        final String expectedCcWithWarningsPayload = readFile("expected/expected_cc_prosecution_received_with_offence_not_in_effect_warnings.json");
        verifyCCWithWarningsEventAndProgressionCommand(staticPayLoad, expectedProgressionPayload, EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS, expectedCcWithWarningsPayload);
    }

    @Test
    void verifyProgressionProsecutionCaseCreated() {
        initiateCCProsecution();

        final SummonsHelper summonsHelper = new SummonsHelper();
        sendPublicEvent(EventSelector.PUBLIC_PROGRESSION_CASE_CREATED_EVENT, "stub-data/public.progression.prosecution-case-created.json", caseId.toString());

        assertThat(summonsHelper.getPrivateEvent(), is(notNullValue()));
        assertThat(summonsHelper.getPublicEvent(), is(notNullValue()));
    }


    @Test
    void shouldAddDefendantsWhenSubsequentSPIMessageReceived() {
        initiateCCProsecution();

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-subsequent-message.json");
        final String ccPayLoad = replaceValues(staticPayLoad);
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_ADDED);
        assertThat(privateEvent.isPresent(), is(true));
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED});
    }

    @Test
    void shouldDeduplicateDefendantsWhenSPIMessageReceivedWithDuplicateDefendants() {
        initiateCCProsecution();

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-subsequent-message.json");
        final String ccPayLoad = replaceValues(staticPayLoad);
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> jsonEnvelope = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_DEFENDANT_ADDED);
        assertThat(jsonEnvelope.isPresent(), is(true));
        assertThat(jsonEnvelope.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_DEFENDANT_ADDED),
                payload().isJson(allOf(
                        withJsonPath("$.defendants[0].id", is(defendantId3))
                ))));
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED});
    }

    @Test
    void shouldNotDeduplicateDefendantsWhenCPPIMessageReceivedWithDuplicateDefendants() {

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-with-duplicate-defendants.json");
        final String ccPayLoad = replaceValues(staticPayLoad, "C", "CPPI");
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> jsonEnvelope = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED);
        assertThat(jsonEnvelope.isPresent(), is(true));
        assertThat(jsonEnvelope.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED),
                payload().isJson(allOf(
                        withJsonPath("$.prosecutionWithReferenceData.prosecution.defendants[*]", hasSize(3)),
                        withJsonPath("$.prosecutionWithReferenceData.prosecution.defendants[0].id", is(defendantId1)),
                        withJsonPath("$.prosecutionWithReferenceData.prosecution.defendants[1].id", is(defendantId2)),
                        withJsonPath("$.prosecutionWithReferenceData.prosecution.defendants[2].id", is(defendantId3))
                ))));
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(new String[]{PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED});
    }

    @Test
    void shouldCreateDefendantsAddedEventWhenInitiationCodeIsNotSummons() {
        initiateCCProsecution();
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-subsequent-message.json");
        final String ccPayLoad = replaceValues(staticPayLoad);
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        initiateCCProsecutionHelper.verifyEventRaised(EVENT_SELECTOR_DEFENDANT_ADDED);

        assertProgressionCommandForAddingDefendant();
    }
    @Test
    void shouldCreateParkedEventInsteadOfDefendantsAddedEventWhenSummonsInitiationCodeIsGiven() {

        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution.json");
        String ccPayLoad = replaceValues(staticPayLoad, "S", "SPI");
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> jsonEnvelope = initiateCCProsecutionHelper.retrieveEvent(EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL);
        assertThat(jsonEnvelope.isPresent(), is(true));

        assertThat(jsonEnvelope.get(), jsonEnvelope(
                metadata().withName(EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL),
                payload().isJson(allOf(
                        withJsonPath("$.applicationId", notNullValue()),
                        withJsonPath("$.prosecutionWithReferenceData.prosecution.defendants[*].id", hasItems(defendantId1, defendantId2))
                ))));


        staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-subsequent-message.json", "S");
        ccPayLoad = replaceValues(staticPayLoad, "S", "SPI");
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        final Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL),
                payload().isJson(allOf(
                        withJsonPath("$.applicationId", notNullValue()),
                        withJsonPath("$.prosecutionWithReferenceData.prosecution.defendants[*].id", hasItems(defendantId3))
                ))));
    }

    private void verifyCCEventAndProgressionCommand(final String staticPayLoad,
                                                    final String expectedPayload,
                                                    final String expectedPrivateEvent,
                                                    final String expectedPublicEvent,
                                                    final Optional<String> expectedPublicEventCCPayload) {
        final String ccPayLoad = replaceValues(staticPayLoad);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(expectedPrivateEvent);
        assertThat(privateEvent.isPresent(), is(true));
        expectedPublicEventCCPayload.ifPresent(expectedPublicEventPayload -> {
            final Optional<JsonEnvelope> jsonEnvelope = initiateCCProsecutionHelper.retrieveEvent(expectedPublicEvent);
            assertThat(jsonEnvelope.isPresent(), is(true));
            assertPublicEventPayload(jsonEnvelope.get().payload().toString(), expectedPublicEventPayload);
        });
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, expectedPayload);
    }

    private void verifyCCEventAndProgressionCommandForMCC(final String staticPayLoad,
                                                          final String expectedPayload) {
        final String ccPayLoad = replaceValues(staticPayLoad);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        initiateCCProsecutionHelper.thenProsecutionReceivedEventShouldBeRaised();
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(
                new String[]{
                        PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED,
                        PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED
                });
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(defendantId1, expectedPayload);
    }

    private void verifyBadRequestPayload(final String staticPayLoad) {
        final String ccPayLoad = replaceValues(staticPayLoad);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecutionWithBadRequest(ccPayLoad);
    }

    private void verifyCCWithWarningsEventAndProgressionCommand(final String staticPayLoad,
                                                                final String expectedProgressionPayload,
                                                                final String expectedPrivateEvent,
                                                                final String expectedCcWithWarningsPayload) {
        final String ccPayLoad = replaceValues(staticPayLoad);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);
        Optional<JsonEnvelope> privateEvent = initiateCCProsecutionHelper.retrieveEvent(expectedPrivateEvent);
        assertThat(privateEvent.isPresent(), is(true));

        assertEquals(expectedCcWithWarningsPayload, privateEvent.get().payload().toString(), new CustomComparator(STRICT,
                new Customization("prosecutionWithReferenceData.prosecution.caseDetails.caseId", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.prosecution.caseDetails.prosecutorCaseReference", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.prosecution.defendants[0].id", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.prosecution.defendants[0].offences[0].offenceId", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.prosecution.externalId", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.externalId", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[0].values[0].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[0].values[1].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[0].values[2].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[1].values[0].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[1].values[1].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[1].values[2].id", (o1, o2) -> true),
                new Customization("id", (o1, o2) -> true)
        ));
        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, replaceValues(expectedProgressionPayload));
    }

    private void verifyCCWithWarningsEventAndProgressionCommandForMCC(final String staticPayLoad,
                                                                      final String expectedProgressionPayload,
                                                                      final String expectedCcWithWarningsPayload) {
        verifyCCWithWarningsEventAndProgressionCommandForMCC(staticPayLoad, expectedProgressionPayload, expectedCcWithWarningsPayload, false);
    }

    private void verifyCCWithWarningsEventAndProgressionCommandForMCC(final String staticPayLoad,
                                                                      final String expectedProgressionPayload,
                                                                      final String expectedCcWithWarningsPayload,
                                                                      final boolean isDefendantOrganisation) {
        final String ccPayLoad = replaceValuesForMCC(staticPayLoad);
        final InitiateCCProsecutionHelper initiateCCProsecutionHelper = new InitiateCCProsecutionHelper();
        initiateCCProsecutionHelper.initiateCCProsecution(ccPayLoad);

        final Optional<JsonEnvelope> actualEventEnvelope = initiateCCProsecutionHelper.retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS);
        assertThat(actualEventEnvelope.isPresent(), is(true));
        initiateCCProsecutionHelper.thenEventsShouldBeRaised(
                new String[]{
                        PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED,
                        PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED
                });

        assertEquals(expectedCcWithWarningsPayload, actualEventEnvelope.get().payload().toString(), new CustomComparator(STRICT,
                new Customization("prosecutionWithReferenceData.prosecution.caseDetails.caseId", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.prosecution.caseDetails.prosecutorCaseReference", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.prosecution.defendants[0].id", (o1, o2) -> true),
                new Customization("prosecutionWithReferenceData.prosecution.defendants[0].offences[0].offenceId", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[0].values[0].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[0].values[1].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[0].values[2].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[1].values[0].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[1].values[1].id", (o1, o2) -> true),
                new Customization("defendantWarnings[0].problems[1].values[2].id", (o1, o2) -> true),
                new Customization("id", (o1, o2) -> true)));

        initiateCCProsecutionHelper.verifyCourtProceedingsForCaseCreationHasBeenInitiated(caseUrn, replaceValuesForMCC(expectedProgressionPayload), isDefendantOrganisation);
    }

    private String replaceValues(final String payload) {
        return replaceValues(payload, "C", "SPI");
    }

    private String replaceValues(final String payload, final String initiationCode, final String channel) {
        return payload
                .replace("CASE-ID", this.caseId.toString())
                .replace("CHANNEL", channel)
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_REFERENCE1", this.defendantId1)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_REFERENCE2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("DEFENDANT_REFERENCE3", this.defendantId3)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("OFFENCE_ID3", this.offenceId3)
                .replace("OFFENCE_ID4", this.offenceId4)
                .replace("OFFENCE_ID5", this.offenceId5)
                .replace("OFFENCE_ID6", this.offenceId6)
                .replace("INITIATION_CODE", initiationCode)
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("DATE_OF_HEARING", LocalDates.to(LocalDate.now().plusMonths(2)))
                .replace("EXTERNAL_ID", this.externalId.toString());
    }

    private String replaceValuesForMCC(final String payload) {
        return payload
                .replace("CASE-ID", this.caseId.toString())
                .replace("CASE-URN", caseUrn)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("OFFENCE_ID3", this.offenceId3)
                .replace("OFFENCE_ID4", this.offenceId4)
                .replace("OFFENCE_ID5", this.offenceId5)
                .replace("OFFENCE_ID6", this.offenceId6);

    }

    private void assertPublicEventPayload(final String actualPayload, final String expectedPublicEventCCPayload) {
        assertEquals(expectedPublicEventCCPayload, actualPayload, new CustomComparator(STRICT,
                new Customization("caseDetails.caseId", (o1, o2) -> true),
                new Customization("caseDetails.prosecutorCaseReference", (o1, o2) -> true),
                new Customization("caseDetails.prosecutor.prosecutionAuthorityId", (o1, o2) -> true),
                new Customization("defendants[0].id", (o1, o2) -> true),
                new Customization("defendants[0].prosecutorDefendantReference", (o1, o2) -> true),
                new Customization("defendants[1].id", (o1, o2) -> true),
                new Customization("defendants[1].prosecutorDefendantReference", (o1, o2) -> true),
                new Customization("defendants[0].offences[0].id", (o1, o2) -> true),
                new Customization("defendants[1].offences[0].id", (o1, o2) -> true)
        ));
    }


    private void assertProgressionCommandForAddingDefendant() {
        await().timeout(35, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .pollDelay(500, TimeUnit.MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching(ADD_DEFENDANT_TO_COURT_PROCEEDING))).size(),
                        CoreMatchers.is(not(0)));
    }

    @Test
    void initiateCCProsecutionForOrganisation() {
        final String staticPayLoad = readFile("command-json/prosecutioncasefile.command.initiate-cc-prosecution-organisation.json");
        final String expectedPayload = readFile("expected/initiate_cc_corporate_expected_organisation_output.json");
        verifyCCEventAndProgressionCommand(staticPayLoad, expectedPayload, EVENT_SELECTOR_CC_PROSECUTION_RECEIVED, PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED, empty());

        final ResponseData responseData = poll(getCaseDetailsByProsecutionReferenceIdBuilder(caseUrn))
                .until(
                        status().is(Response.Status.OK));

        assertThat(responseData.getStatus(), equalTo(Response.Status.OK));
    }
}
