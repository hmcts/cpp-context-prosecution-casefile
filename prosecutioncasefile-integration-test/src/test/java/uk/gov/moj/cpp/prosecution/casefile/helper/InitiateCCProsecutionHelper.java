package uk.gov.moj.cpp.prosecution.casefile.helper;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createReader;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.bigDecimal;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.randomEnum;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_CREATED_SUCCESSFULLY;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_RECEIVED_WITH_DUPLICATE_DEFENDANTS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_CC_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROGRESSION_CASE_DEFENDANT_CHANGED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubForQueryApplication;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeWithEitherWayModeOfTrial;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetOrganisationUnitsReturnsEmptyList;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubProsecutorsReturns404;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecution.casefile.event.DefendantsParkedForSummonsApplicationApproval;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonReader;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class InitiateCCProsecutionHelper extends AbstractTestHelper {
    private static final String PROGRESSION_INITIATE_COURT_PROCEEDINGS = "progression.initiate-court-proceedings";
    private static final String CASE_MARKER_CODE = "ABC";

    private final UUID caseId;
    private final String caseUrn;
    private final String defendantId1;
    private final String defendantId2;
    private final String defendantId3;
    private final String prosecutorDefendantReference1;
    private final String prosecutorDefendantReference2;
    private final String prosecutorDefendantReference3;
    private final String offenceId1;
    private final String offenceId2;
    private final String offenceId3;
    private final String offenceId4;
    private final String offenceId5;
    private final String offenceId6;
    private final UUID externalId;
    private final UUID externalId2;
    private final String prosecutorCost;
    private final boolean summonsSuppressed;
    private final boolean personalService;

    private UUID applicationId;
    private UUID applicationId2;
    private List<String> defendantIds;
    private List<String> defendantIds2;

    public InitiateCCProsecutionHelper() {
        caseId = randomUUID();
        caseUrn = randomAlphanumeric(10);
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        defendantId3 = randomUUID().toString();
        prosecutorDefendantReference1 = "TFLDEF001";
        prosecutorDefendantReference2 = "TFLDEF002";
        prosecutorDefendantReference3 = "TFLDEF003";
        offenceId1 = randomUUID().toString();
        offenceId2 = randomUUID().toString();
        offenceId3 = randomUUID().toString();
        offenceId4 = randomUUID().toString();
        offenceId5 = randomUUID().toString();
        offenceId6 = randomUUID().toString();
        externalId = randomUUID();
        externalId2 = randomUUID();
        prosecutorCost = format("£%s", bigDecimal(100, 10000, 2).next());
        summonsSuppressed = BOOLEAN.next();
        personalService = BOOLEAN.next();

        createPrivateConsumerForMultipleSelectors(
                EVENT_SELECTOR_CASE_CREATED_SUCCESSFULLY,
                EVENT_SELECTOR_CC_PROSECUTION_RECEIVED_WITH_WARNINGS,
                EVENT_SELECTOR_CC_PROSECUTION_RECEIVED,
                PROSECUTIONCASEFILE_HANDLER_CASE_UPDATED_INITIATE_IDPC_MATCH,
                EVENT_SELECTOR_CASE_VALIDATION_FAILED,
                EVENT_SELECTOR_DEFENDANT_VALIDATION_FAILED,
                EVENT_SELECTOR_CC_PROSECUTION_REJECTED,
                EVENT_SELECTOR_DEFENDANT_ADDED,
                EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED,
                EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL,
                EVENT_SELECTOR_CASE_RECEIVED_WITH_DUPLICATE_DEFENDANTS);

        createPublicConsumerForMultipleSelectors(
                PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED_WITH_WARNINGS,
                PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED,
                PUBLIC_PROSECUTIONCASEFILE_CC_CASE_RECEIVED,
                PUBLIC_PROSECUTIONCASEFILE_CASE_VALIDATION_FAILED,
                PUBLIC_PROSECUTIONCASEFILE_DEFENDANT_VALIDATION_FAILED,
                PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED,
                PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_DEFENDANTS_ADDED,
                PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED);
    }

    public static String getLastLoggedRequest(final String caseUrn) {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                .withRequestBody(containing(caseUrn)));

        return loggedRequests.get(loggedRequests.size() - 1).getBodyAsString();
    }

    public static String getLastLoggedRequestForSummonsApplicationApproval(final String caseUrn) {
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiate-application"))
                .withRequestBody(containing(caseUrn)));

        return loggedRequests.get(loggedRequests.size() - 1).getBodyAsString();
    }

    public void verifyCourtProceedingsForSummonsApplicationHasBeenInitiated(final String expectedPayloadPath) {
        verifyCourtProceedingsForSummonsApplicationHasBeenInitiated(this.caseUrn, expectedPayloadPath);
    }

    public void verifyPublicEventRaisedForManualCaseReceivedForMCCChannel() {
        assertThat(retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED).get(), jsonEnvelope(
                metadata().withName(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.applicationId", is(applicationId.toString()))))));
    }

    public void verifyCourtProceedingsForSummonsApplicationHasBeenInitiated(final String containedString, final String expectedPayloadPath) {
        final String expectedPayload = replaceValues(readFile(expectedPayloadPath), randomEnum(Channel.class).next().name());
        await().timeout(35, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .pollDelay(500, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiate-application"))
                                .withRequestBody(containing(containedString))).size(), is(1));

        final JsonObject jsonObject;
        try (final JsonReader jsonReader = createReader(new StringReader(getLastLoggedRequestForSummonsApplicationApproval(containedString)))) {
            jsonObject = jsonReader.readObject();
        }

        final DefaultJsonObjectEnvelopeConverter jsonObjectEnvelopeConverter = new DefaultJsonObjectEnvelopeConverter();

        final String actualPayload = jsonObjectEnvelopeConverter.extractPayloadFromEnvelope(jsonObject).toString();
        assertEquals(expectedPayload, actualPayload, new CustomComparator(STRICT,
                new Customization("courtApplication.applicant.id", (o1, o2) -> true))
        );
    }

    public void verifyCourtProceedingsForCaseCreationHasBeenInitiated(final String containedString, final String expectedPayloadTemplate) {
        verifyCourtProceedingsForCaseCreationHasBeenInitiated(containedString, expectedPayloadTemplate, false);
    }

    public void verifyCourtProceedingsForCaseCreationHasBeenInitiated(final String containedString, final String expectedPayloadTemplate, final boolean isDefendantOrganisation) {
        await().timeout(35, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .pollDelay(500, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(containedString))).size(), is(1));


        final JsonObject jsonObject;
        try (final JsonReader jsonReader = createReader(new StringReader(getLastLoggedRequest(containedString)))) {
            jsonObject = jsonReader.readObject();
        }

        final DefaultJsonObjectEnvelopeConverter defaultJsonObjectEnvelopeConverter = new DefaultJsonObjectEnvelopeConverter();

        assertThat(PROGRESSION_INITIATE_COURT_PROCEEDINGS, is(defaultJsonObjectEnvelopeConverter.asEnvelope(jsonObject).metadata().name()));

        final String actualPayload = defaultJsonObjectEnvelopeConverter.extractPayloadFromEnvelope(jsonObject).toString();
        final String expectedPayload = replaceWithApplicationResults(expectedPayloadTemplate);

        assertEquals(expectedPayload, actualPayload, new CustomComparator(STRICT, getCustomAsserts(isDefendantOrganisation).toArray(new Customization[0])));
    }

    private List<Customization> getCustomAsserts(final boolean isDefendantOrganisation) {
        final List<Customization> customizedAsserts = new ArrayList<>();
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].defendantId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].prosecutionCaseId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].defendantOffences[0]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].defendantOffences[1]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[1].defendantId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[1].prosecutionCaseId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[1].defendantOffences[0]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[1].defendantOffences[1]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[2].defendantId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[2].prosecutionCaseId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[2].defendantOffences[0]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[2].defendantOffences[1]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].listedEndDateTime", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.listHearingRequests[0].courtScheduleId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].civilFees[0]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].civilFees[1]", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].isCivil", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].cpsOrganisation", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].prosecutionCaseIdentifier.caseURN", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].pncId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].croNumber", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].masterDefendantId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].courtProceedingsInitiated", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].masterDefendantId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].courtProceedingsInitiated", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].masterDefendantId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].courtProceedingsInitiated", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].prosecutionCaseId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].prosecutionCaseId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].prosecutionCaseId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].plea.offenceId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].maxPenalty", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].verdict.offenceId", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].verdict.verdictType.description", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[1].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].offences[0].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].offences[1].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].offences[1].civilOffence", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].offences[0].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].offences[0].civilOffence", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].offences[1].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].offences[1].civilOffence", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].caseMarkers[0].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].caseMarkers[1].id", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].personDefendant.bailStatus", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].personDefendant.bailStatus", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].allocationDecision", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].offences[0].allocationDecision", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].offences[1].allocationDecision", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].offences[2].allocationDecision", (o1, o2) -> true));

        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].offences[0].allocationDecision", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].offences[1].allocationDecision", (o1, o2) -> true));
        customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].offences[2].allocationDecision", (o1, o2) -> true));

        customizedAsserts.add(new Customization("id", (o1, o2) -> true));


        if (!isDefendantOrganisation) {
            customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].prosecutionAuthorityReference", (o1, o2) -> true));
            customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[1].prosecutionAuthorityReference", (o1, o2) -> true));
            customizedAsserts.add(new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[2].prosecutionAuthorityReference", (o1, o2) -> true));
        }

        return customizedAsserts;
    }

    public void verifyCourtProceedingsForCaseCreationHasBeenInitiatedForMcc(final String containedString, final String expectedPayloadTemplate) {
        await().timeout(35, SECONDS)
                .pollInterval(500, MILLISECONDS)
                .pollDelay(500, MILLISECONDS)
                .until(
                        () -> findAll(postRequestedFor(urlMatching("/progression-service/command/api/rest/progression/initiatecourtproceedings"))
                                .withRequestBody(containing(containedString))).size(), is(1));


        final JsonObject jsonObject;
        try (final JsonReader jsonReader = createReader(new StringReader(getLastLoggedRequest(containedString)))) {
            jsonObject = jsonReader.readObject();
        }

        final DefaultJsonObjectEnvelopeConverter defaultJsonObjectEnvelopeConverter = new DefaultJsonObjectEnvelopeConverter();

        assertThat(PROGRESSION_INITIATE_COURT_PROCEEDINGS, is(defaultJsonObjectEnvelopeConverter.asEnvelope(jsonObject).metadata().name()));

        final String actualPayload = defaultJsonObjectEnvelopeConverter.extractPayloadFromEnvelope(jsonObject).toString();
        final String expectedPayload = replaceWithApplicationResults(expectedPayloadTemplate);

        assertEquals(expectedPayload, actualPayload, new CustomComparator(STRICT,
                new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].defendantId", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].prosecutionCaseId", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].defendantOffences[0]", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.listHearingRequests[0].listDefendantRequests[0].defendantOffences[1]", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].id", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].civilFees[0]", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].civilFees[1]", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].isCivil", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].cpsOrganisation", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].prosecutionCaseIdentifier.caseURN", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].id", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].prosecutionAuthorityReference", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].pncId", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].croNumber", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].masterDefendantId", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].courtProceedingsInitiated", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].prosecutionCaseId", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].id", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].plea.offenceId", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].maxPenalty", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].verdict.offenceId", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[1].id", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].caseMarkers[0].id", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].caseMarkers[1].id", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].personDefendant.bailStatus", (o1, o2) -> true),
                new Customization("initiateCourtProceedings.prosecutionCases[0].defendants[0].offences[0].allocationDecision", (o1, o2) -> true),
                new Customization("id", (o1, o2) -> true)
        ));
    }

    private String replaceWithApplicationResults(final String expectedPayloadTemplate) {
        return expectedPayloadTemplate
                .replaceAll("OFFENCE_ID1", this.offenceId1)
                .replaceAll("OFFENCE_ID2", this.offenceId2)
                .replaceAll("PERSONAL_SERVICE", String.valueOf(this.personalService))
                .replaceAll("PROSECUTOR_COST", this.prosecutorCost)
                .replaceAll("SUMMONS_SUPPRESSED", String.valueOf(this.summonsSuppressed));
    }

    public void initiateCCProsecution(final String payload) {
        makePostCall(getWriteUrl("/cc-prosecution"),
                "application/vnd.prosecutioncasefile.command.initiate-cc-prosecution+json",
                payload);
    }

    public void initiateCCProsecutionWithBadRequest(final String payload) {
        makePostCallWithBadRequest(getWriteUrl("/cc-prosecution"),
                "application/vnd.prosecutioncasefile.command.initiate-cc-prosecution+json",
                payload);
    }

    public void initiateSummonsCaseForChannelAndVerifyApplicationCreatedInstead(final Channel channel, final String payloadPath, final String expectedPayloadPath) {
        whenInitiateSummonsCaseIsRaisedByChannel(channel, payloadPath);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent(EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL);
        assertThat(jsonEnvelope.isPresent(), is(true));
        final DefendantsParkedForSummonsApplicationApproval payload = jsonObjectToObjectConverter.convert(jsonEnvelope.get().payloadAsJsonObject(), DefendantsParkedForSummonsApplicationApproval.class);
        this.applicationId = payload.getApplicationId();
        stubForQueryApplication(applicationId);
        this.defendantIds = payload.getProsecutionWithReferenceData().getProsecution().getDefendants().stream()
                .map(Defendant::getId)
                .collect(toList());

        verifyCourtProceedingsForSummonsApplicationHasBeenInitiated(expectedPayloadPath);
    }

    public void initiateSubsequentSummonsCaseForChannelAndVerifyApplicationCreatedInstead(final Channel channel, final String payloadPath, final String expectedPayloadPath) {
        whenInitiateSummonsCaseIsRaisedByChannel(channel, payloadPath);

        final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent(EVENT_DEFENDANTS_PARKED_FOR_SUMMONS_APPLICATION_APPROVAL);
        assertThat(jsonEnvelope.isPresent(), is(true));
        final DefendantsParkedForSummonsApplicationApproval payload = jsonObjectToObjectConverter.convert(jsonEnvelope.get().payloadAsJsonObject(), DefendantsParkedForSummonsApplicationApproval.class);
        this.applicationId2 = payload.getApplicationId();
        this.defendantIds2 = payload.getProsecutionWithReferenceData().getProsecution().getDefendants().stream()
                .map(Defendant::getId)
                .collect(toList());

        verifyCourtProceedingsForSummonsApplicationHasBeenInitiated(applicationId2.toString(), expectedPayloadPath);
    }

    public void whenInitiateSummonsCaseIsRaisedByChannel(final Channel channel, final String payloadPath) {
        createAdditionalMockEndpoints();
        final String templatePayload = readFile(payloadPath);
        final String payload = replaceValues(templatePayload, channel.toString());
        initiateCCProsecution(payload);
    }

    public void givenInitiateSummonsCaseIsRaisedWithErrorsByChannel(final String channel) {
        stubProsecutorsReturns404();
        stubGetOrganisationUnitsReturnsEmptyList();

        final String templatePayload = readFile("command-json/prosecutioncasefile.command.initiate-channel-parametric-summons-prosecution-with-error.json");
        final String payload = replaceValues(templatePayload, channel);
        initiateCCProsecution(payload);
    }

    public void whenSummonsApplicationIsRejectedForDefendants() {
        sendPublicEvent(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_REJECTED,
                "stub-data/public.progression.court-application-summons-rejected.json",
                this.caseId.toString(), this.applicationId.toString(), this.defendantIds.stream().map(id -> format("\"%s\"", id)).collect(joining(",")));
    }

    public void whenSummonsApplicationIsApprovedForDefendants() {
        sendPublicEvent(PUBLIC_PROGRESSION_COURT_APPLICATION_SUMMONS_APPROVED,
                "stub-data/public.progression.court-application-summons-approved.json",
                this.applicationId.toString(), this.caseId.toString(), this.prosecutorCost, String.valueOf(this.summonsSuppressed), String.valueOf(this.personalService));
    }

    public void whenCaseDefendantChanged(final String defendantId) {
        sendPublicEvent(PUBLIC_PROGRESSION_CASE_DEFENDANT_CHANGED,
                "stub-data/public.progression.case-defendant-changed.json", defendantId);
    }

    public void thenProsecutionShouldRejectTheCase(final String expectedPublicEvent, final String expectedPayloadPath) {
        final Optional<JsonEnvelope> eventRaised = retrieveEvent(expectedPublicEvent);
        assertThat(eventRaised.isPresent(), is(true));

        final String expectedPayload = readFile(expectedPayloadPath)
                .replace("CASE-ID", this.caseId.toString())
                .replace("CASE-URN", this.caseUrn)
                .replace("EXTERNAL_ID", this.externalId.toString());
        final String actualPayload = eventRaised.get().payloadAsJsonObject().toString();
        assertEquals(expectedPayload, actualPayload, STRICT);
    }

    public void thenEventsShouldBeRaised(final String[] expectedEvents) {
        Arrays.stream(expectedEvents).forEach(eventName -> {
            final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent(eventName);
            assertThat(jsonEnvelope.isPresent(), is(true));
        });
    }

    public void thenPublicEventsShouldBeRaised(final String expectedPublicEvent, final Matcher matcher) {
        final Optional<JsonEnvelope> jsonEnvelope = retrieveMessageWithMatchers(expectedPublicEvent, matcher);
        assertThat(jsonEnvelope.isPresent(), is(true));
    }

    public JsonEnvelope verifyEventRaised(final String eventName) {
        final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent(eventName);
        assertThat(jsonEnvelope.isPresent(), is(true));
        return jsonEnvelope.get();
    }

    public void sendPublicEvent(final String name, final String payloadResource, final String... placeholders) {
        final JsonEnvelope publicEvent = envelopeFrom(
                metadataWithRandomUUID(name),
                readJsonResource(payloadResource, (Object[]) placeholders));

        sendMessage(name, publicEvent);
    }

    private void createAdditionalMockEndpoints() {
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
        stubOffencesForOffenceCodeWithEitherWayModeOfTrial();
    }

    private String replaceValues(final String payload, final String channel) {
        String resultPayload = payload;

        //only 1 application ID should be updated
        if (nonNull(this.applicationId2)) {
            resultPayload = payload.replace("APPLICATION_ID_2", this.applicationId2.toString());
        } else if (nonNull(this.applicationId)) {
            resultPayload = payload.replace("APPLICATION_ID", this.applicationId.toString());
        }

        return resultPayload
                .replace("CASE-ID", this.caseId.toString())
                .replace("CASE-URN", caseUrn)
                .replace("PROSECUTOR_DEFENDANT_REFERENCE1", this.prosecutorDefendantReference1)
                .replace("PROSECUTOR_DEFENDANT_REFERENCE2", this.prosecutorDefendantReference2)
                .replace("PROSECUTOR_DEFENDANT_REFERENCE3", this.prosecutorDefendantReference3)
                .replace("DEFENDANT_ID1", this.defendantId1)
                .replace("DEFENDANT_ID2", this.defendantId2)
                .replace("DEFENDANT_ID3", this.defendantId3)
                .replace("OFFENCE_ID1", this.offenceId1)
                .replace("OFFENCE_ID2", this.offenceId2)
                .replace("OFFENCE_ID3", this.offenceId3)
                .replace("OFFENCE_ID4", this.offenceId4)
                .replace("OFFENCE_ID5", this.offenceId5)
                .replace("OFFENCE_ID6", this.offenceId6)
                .replace("CASE_MARKER", CASE_MARKER_CODE)
                .replace("CHANNEL", channel)
                .replace("DATE_RECEIVED", LocalDates.to(LocalDate.now()))
                .replace("EXTERNAL_ID_2", this.externalId2.toString())
                .replace("EXTERNAL_ID", this.externalId.toString())
                .replaceAll("DATE_OF_HEARING", LocalDates.to(LocalDate.now()))
                .replaceAll("APPLICATION_DUE_DATE", LocalDates.to(LocalDate.now()));
    }

    public JsonEnvelope thenProsecutionReceivedEventShouldBeRaised() {
        final Optional<JsonEnvelope> jsonEnvelope = retrieveEvent(EVENT_SELECTOR_CC_PROSECUTION_RECEIVED);
        assertThat(jsonEnvelope.isPresent(), is(true));
        return jsonEnvelope.get();
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
