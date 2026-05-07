package uk.gov.moj.cpp.prosecution.casefile.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.prosecution.casefile.helper.DefaultRequests.getCaseDetailsBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.helper.DefaultRequests.getCaseDetailsByProsecutionReferenceIdBuilder;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_INITIATED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.EVENT_SELECTOR_SJP_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_CASE_UNSUPPORTED;
import static uk.gov.moj.cpp.prosecution.casefile.helper.EventSelector.PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS;
import static uk.gov.moj.cpp.prosecution.casefile.helper.FileUtil.readJsonResource;
import static uk.gov.moj.cpp.prosecution.casefile.helper.ValidationErrorHelper.queryAndVerifyCaseErrors;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel.SPI;
import static uk.gov.moj.cpp.prosecution.casefile.stub.CreateSjpCaseStub.getSentCreateSjpCaseCommand;
import static uk.gov.moj.cpp.prosecution.casefile.stub.CreateSjpCaseStub.resetAndStubCreateSjpCase;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubEndorsableOffencesForOffenceCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataStub.stubGetCaseMarkersWithCode;
import static uk.gov.moj.cpp.prosecution.casefile.stub.TestUtils.readFile;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.ALCOHOL_DRUG_LEVEL_METHOD_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_DOB_IN_FUTURE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_NATIONALITY_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DEFENDANT_UNDER_18_YEARS_AT_CHARGE_DATE;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.DUPLICATED_PROSECUTION;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.OFFENCE_CODE_IS_INVALID;
import static uk.gov.moj.cpp.prosecution.casefile.validation.Problems.newProblem;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_DOB;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.DEFENDANT_NATIONALITY;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_ALCOHOL_LEVEL_METHOD;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CODE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher;
import uk.gov.moj.cpp.prosecution.casefile.helper.InitiateSjpProsecutionHelper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.ArrayValueMatcher;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class InitiateSjpProsecutionIT extends BaseIT {
    private static final String CASE_MARKER_CODE = "ABC";
    private static final String ALCOHOL_LEVEL_METHOD_INVALID = "D1";
    private static final String NATIONALITY_VALID = "GBR";
    private static final String NATIONALITY_INVALID = "BRT";
    private static final String OFFENCE_CODE_12 = "OFCODE12";
    private static final String FIELD_EXTERNAL_ID = "externalId";
    public static final String GAEAA_01 = "GAEAA01";

    private UUID caseId;
    private UUID externalId;
    private String channel;
    private String caseUrn;
    private String policeSystemId;

    final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper = new InitiateSjpProsecutionHelper();

    @BeforeAll
    public static void setupOnce() {
        stubWireMocks();
    }

    @BeforeEach
    public void setUp() {
        caseId = randomUUID();
        caseUrn = randomAlphanumeric(10);
        externalId = randomUUID();
        policeSystemId = randomAlphanumeric(8);
        channel = "CPPI";

        stubOffencesForOffenceCode();
    }

    private static void stubWireMocks() {
        resetAndStubCreateSjpCase();
        stubGetCaseMarkersWithCode(CASE_MARKER_CODE);
    }

    private static void assertRejectionResponse(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final UUID caseId, final Matcher<ReadContext> prosecutionRejectionPayloadMatcher) {
        final Optional<JsonEnvelope> privateEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_REJECTED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_SJP_PROSECUTION_REJECTED),
                payload().isJson(allOf(
                        withJsonPath("$.prosecution", notNullValue()),
                        withJsonPath("$.prosecution.caseDetails.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.errors[*]", prosecutionRejectionPayloadMatcher)
                ))));

        final Optional<JsonEnvelope> publicEvent = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(publicEvent.get(), jsonEnvelope(
                metadata().withName(PUBLIC_EVENT_SELECTOR_PROSECUTION_REJECTED),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.errors[*]", prosecutionRejectionPayloadMatcher)
                ))));
    }

    private static void assertRejectionResponseForMCC(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final UUID caseId, final Matcher<ReadContext> prosecutionRejectionPayloadMatcher) {
        final Optional<JsonEnvelope> privateEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_REJECTED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_SJP_PROSECUTION_REJECTED),
                payload().isJson(allOf(
                        withJsonPath("$.prosecution", notNullValue()),
                        withJsonPath("$.prosecution.caseDetails.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.errors[*]", prosecutionRejectionPayloadMatcher)
                ))));

        final Optional<JsonEnvelope> publicEvent = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(publicEvent.get(), jsonEnvelope(
                metadata().withName(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED),
                payload().isJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.errors[*]", prosecutionRejectionPayloadMatcher)
                ))));
    }

    private static void assertProsecutionCaseUnsupportedResponseForSPI(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, String> keyValues) {
        final Optional<JsonEnvelope> privateEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED);
        assertThat(privateEvent.isPresent(), is(true));

        assertThat(privateEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_PROSECUTION_CASE_UNSUPPORTED),
                payload().isJson(allOf(
                        withJsonPath("$.channel", is(SPI.name())),
                        withJsonPath("$.errorMessage", is(keyValues.get("errorMessage"))),
                        withJsonPath("$.urn", is(keyValues.get("urn"))),
                        withJsonPath("$.externalId", is(keyValues.get("externalId"))),
                        withJsonPath("$.policeSystemId", is(keyValues.get("policeSystemId"))
                        )))));

        final Optional<JsonEnvelope> publicEvent = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_CASE_UNSUPPORTED);
        assertThat(publicEvent.isPresent(), is(true));

        assertThat(publicEvent.get(), jsonEnvelope(
                metadata().withName(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_CASE_UNSUPPORTED),
                payload().isJson(allOf(
                        withJsonPath("$.channel", is(SPI.name())),
                        withJsonPath("$.errorMessage", is(keyValues.get("errorMessage"))),
                        withJsonPath("$.urn", is(keyValues.get("urn"))),
                        withJsonPath("$.externalId", is(keyValues.get("externalId"))),
                        withJsonPath("$.policeSystemId", is(keyValues.get("policeSystemId"))
                        )))));
    }

    private static Matcher getProblemsMatcher(final Problem... problems) {
        return allOf(Stream.of(problems).map(problem ->
                hasItem(isJson(allOf(
                        withJsonPath("code", is(problem.getCode())),
                        withJsonPath("values[0].key", is(problem.getValues().get(0).getKey())),
                        withJsonPath("values[0].value", is(problem.getValues().get(0).getValue()))
                )))).collect(toList()));
    }

    private void emitSjpCaseCreatedEvent(final UUID caseId) {
        final JsonEnvelope sjpCaseCreatedEvent = envelopeFrom(
                metadataWithRandomUUID("public.sjp.sjp-case-created"),
                readJsonResource("stub-data/public.sjp.sjp-case-created.json", caseId));

        sendPublicEvent("public.sjp.sjp-case-created", sjpCaseCreatedEvent);
    }

    private static void assertSame(final JsonObject jsonObject1, final JsonObject jsonObject2) {
        assertEquals(jsonObject1.toString(), jsonObject2.toString(), LENIENT);
    }


    @Test
    public void initiateSjpProsecutionAndRejectDuplicateProsecution() {
        final LocalDate dateOfBirth = now().minusYears(30);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2018, 2, 1);
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 1, 1);

        final UUID expectedDefendantId = UUID.randomUUID();



        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12, GAEAA_01);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));

        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");

        // check private event prosecution-received raised
        final JsonObject initiateSjpProsecutionHandlerRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data.json",
                        ImmutableMap.<String, Object>builder()
                                .put("case.id", caseId)
                                .put("case.urn", caseUrn)
                                .put("offence.id", offenceId)
                                .put("offence.chargeDate", offenceChargeDate)
                                .put("defendant.id", defendantId)
                                .put("defendant.dob", dateOfBirth)
                                .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                .put("offence.endorsable", false)
                                .put("external.id", externalId)
                                .build());

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceivedEvent.get().payloadAsJsonObject(), initiateSjpProsecutionHandlerRequest);

        // query case in the viewstore
        initiateSjpProsecutionHelper.verifyCaseDetails(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId, dateOfBirth, offenceId, offenceChargeDate, offenceCommittedDate);

        // behind the flow at this point enterpriseId is generated and the payload decorated.

        // check private event prosecution-initiated raised
        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));

        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        JsonObject prosecution = initiateSjpProsecutionHandlerRequest.getJsonObject("prosecution");
        JsonObject newProsecution = createObjectBuilder(prosecution)
                .remove("externalId")
                .remove("isCivil")
                .remove("isGroupMaster")
                .remove("isGroupMember")
                .build();

        // prosecution-received does not contain enterpriseId & externalId
        final JsonObject expected = createObjectBuilderWithFilter(initiateSjpProsecutionHandlerRequest, field -> !FIELD_EXTERNAL_ID.equalsIgnoreCase(field))
                .remove("prosecution")
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .add("prosecution", newProsecution)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        // check public SJP event raised
        verifyCaseCreatedInSjp(enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate, offenceCommittedDate, defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);

        final Optional<JsonEnvelope> caseCreatedSuccessfullyEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY);
        assertThat(caseCreatedSuccessfullyEvent.isPresent(), is(true));

        assertThat(caseCreatedSuccessfullyEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY),
                payload().isJson(withJsonPath("$.caseId", is(caseId.toString())))));

        // check that attempt to initialise twice is rejected
        initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID);

        final Problem duplicatedProsecutionProblem = newProblem(DUPLICATED_PROSECUTION, "urn", caseUrn);

        final Matcher<ReadContext> prosecutionRejectionPayloadMatcher = getProblemsMatcher(duplicatedProsecutionProblem);

        assertRejectionResponse(initiateSjpProsecutionHelper, caseId, prosecutionRejectionPayloadMatcher);
    }

    @Test
    public void initiateSjpProsecutionForOrganisationAndRejectDuplicateProsecution() {
        final LocalDate dateOfBirth = now().minusYears(30);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2018, 2, 1);
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 1, 1);

        final UUID expectedDefendantId = UUID.randomUUID();

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12, GAEAA_01);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");

        // check private event prosecution-received raised
        final JsonObject initiateSjpProsecutionHandlerRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-for-organisation-with-reference-data.json",
                        ImmutableMap.<String, Object>builder()
                                .put("case.id", caseId)
                                .put("case.urn", caseUrn)
                                .put("offence.id", offenceId)
                                .put("offence.chargeDate", offenceChargeDate)
                                .put("defendant.id", defendantId)
                                .put("defendant.dob", dateOfBirth)
                                .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                .put("offence.endorsable", false)
                                .put("external.id", externalId)
                                .build());

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceivedEvent.get().payloadAsJsonObject(), initiateSjpProsecutionHandlerRequest);

        // query case in the viewstore
        initiateSjpProsecutionHelper.verifyCaseDetails(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId, dateOfBirth, offenceId, offenceChargeDate, offenceCommittedDate);

        // behind the flow at this point enterpriseId is generated and the payload decorated.

        // check private event prosecution-initiated raised
        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        JsonObject prosecution = initiateSjpProsecutionHandlerRequest.getJsonObject("prosecution");
        JsonObject newProsecution = createObjectBuilder(prosecution)
                .remove("externalId")
                .remove("isCivil")
                .remove("isGroupMaster")
                .remove("isGroupMember")
                .build();

        // prosecution-received does not contain enterpriseId & externalId
        final JsonObject expected = createObjectBuilderWithFilter(initiateSjpProsecutionHandlerRequest, field -> !FIELD_EXTERNAL_ID.equalsIgnoreCase(field))
                .remove("prosecution")
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .add("prosecution", newProsecution)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        // check public SJP event raised
        verifyCaseCreatedInSjp(enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate, offenceCommittedDate, defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);

        final Optional<JsonEnvelope> caseCreatedSuccessfullyEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY);
        assertThat(caseCreatedSuccessfullyEvent.isPresent(), is(true));

        assertThat(caseCreatedSuccessfullyEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY),
                payload().isJson(withJsonPath("$.caseId", is(caseId.toString())))));

        // check that attempt to initialise twice is rejected
        initiateSjpProsecutionOrganisation(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID);

        final Problem duplicatedProsecutionProblem = newProblem(DUPLICATED_PROSECUTION, "urn", caseUrn);

        final Matcher<ReadContext> prosecutionRejectionPayloadMatcher = getProblemsMatcher(duplicatedProsecutionProblem);

        assertRejectionResponse(initiateSjpProsecutionHelper, caseId, prosecutionRejectionPayloadMatcher);
    }

    @Test
    public void initiateSjpProsecutionWithWarnings() {
        final LocalDate dateOfBirth = now().minusYears(16);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 1, 1);
        final LocalDate offenceChargeDate = LocalDate.of(2018, 7, 10);

        initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, randomUUID(), OFFENCE_CODE_12, GAEAA_01);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");

        final Problem minorDefendantProblem = newProblem(DEFENDANT_UNDER_18_YEARS_AT_CHARGE_DATE, new ProblemValue(null, "dateOfBirth", dateOfBirth.toString()), new ProblemValue(null, "chargeDate", offenceChargeDate.toString()));

        final JsonArray expectedWarnings = getProblem(minorDefendantProblem);

        final JsonObject prosecutionReceivedWithWarnings =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data.json",
                        ImmutableMap.<String, Object>builder()
                                .put("case.id", caseId)
                                .put("case.urn", caseUrn)
                                .put("offence.id", offenceId)
                                .put("defendant.id", defendantId)
                                .put("defendant.dob", dateOfBirth)
                                .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                .put("offence.committedDate", offenceCommittedDate)
                                .put("offence.chargeDate", offenceChargeDate)
                                .put("offence.endorsable", false)
                                .put("offence.pressRestrictable", false)
                                .put("external.id", externalId)
                                .build()

                )).add("warnings", expectedWarnings).build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS));
        JSONAssert.assertEquals(prosecutionReceivedEvent.get().payloadAsJsonObject().toString(), prosecutionReceivedWithWarnings.toString(), new CustomComparator(
                STRICT,
                new Customization("defendantWarnings[0].problems[1].values[0].id", (o1, o2) -> true)
        ));

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        // prosecution-received does not contain enterpriseId
        JsonObject prosecution = prosecutionReceivedWithWarnings.getJsonObject("prosecution");
        JsonObject newProsecution = createObjectBuilder(prosecution)
                .remove("externalId")
                .remove("isCivil")
                .remove("isGroupMaster")
                .remove("isGroupMember")
                .build();
        final JsonObject expected = createObjectBuilderWithFilter(prosecutionReceivedWithWarnings, field -> !FIELD_EXTERNAL_ID.equalsIgnoreCase(field))
                .remove("prosecution")
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .add("warnings", expectedWarnings)
                .add("prosecution", newProsecution)
                .build();


        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp(enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate, offenceCommittedDate, defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);

        final Matcher<ReadContext> prosecutionWarningsPayloadMatcher = getProblemsMatcher(minorDefendantProblem);
        assertWarningsResponse(initiateSjpProsecutionHelper, caseId, prosecutionWarningsPayloadMatcher);
    }

    @Test
    public void initiateSjpProsecutionWithWarningsForMCC() {
        final LocalDate dateOfBirth = now().minusYears(16);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 1, 1);
        final LocalDate offenceChargeDate = LocalDate.of(2018, 7, 10);

        initiateSjpProsecutionForMCC(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, prosecutorDefendantReference, OFFENCE_CODE_12);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");

        final Problem minorDefendantProblem = newProblem(DEFENDANT_UNDER_18_YEARS_AT_CHARGE_DATE, new ProblemValue(null, "dateOfBirth", dateOfBirth.toString()), new ProblemValue(null, "chargeDate", offenceChargeDate.toString()));

        final JsonArray expectedWarnings = getProblem(minorDefendantProblem);

        final JsonObject prosecutionReceivedWithWarnings =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-mcc.json",
                        ImmutableMap.<String, Object>builder()
                                .put("case.id", caseId)
                                .put("case.urn", caseUrn)
                                .put("offence.id", offenceId)
                                .put("defendant.id", defendantId)
                                .put("defendant.dob", dateOfBirth)
                                .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                .put("offence.committedDate", offenceCommittedDate)
                                .put("offence.chargeDate", offenceChargeDate)
                                .put("offence.endorsable", false)
                                .put("offence.pressRestrictable", false)
                                .build()

                )).add("warnings", expectedWarnings).build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS));
        assertSame(prosecutionReceivedEvent.get().payloadAsJsonObject(), prosecutionReceivedWithWarnings);

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS));

        final Optional<JsonEnvelope> publicMCCEvent = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED);
        assertThat(publicMCCEvent.isPresent(), is(true));
        assertThat(publicMCCEvent.get().metadata().name(), equalTo(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        JsonObject prosecution = prosecutionReceivedWithWarnings.getJsonObject("prosecution");
        JsonObject newProsecution = createObjectBuilder(prosecution)
                .remove("externalId")
                .remove("isCivil")
                .remove("isGroupMaster")
                .remove("isGroupMember")
                .build();

        // prosecution-received does not contain enterpriseId
        final JsonObject expected = createObjectBuilder(prosecutionReceivedWithWarnings)
                .remove("prosecution")
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .add("warnings", expectedWarnings)
                .add("prosecution", newProsecution)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp("stub-data/query.sjp-case-without-warnings-mcc.json",
                enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate,
                offenceCommittedDate, defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);

        final Matcher<ReadContext> prosecutionWarningsPayloadMatcher = getProblemsMatcher(minorDefendantProblem);
        assertWarningsResponse(initiateSjpProsecutionHelper, caseId, prosecutionWarningsPayloadMatcher);
    }

    @Test
    @Disabled
    public void initiateSjpProsecutionWithoutWarnings() {
        final LocalDate dateOfBirth = LocalDate.of(1999, 9, 10);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2024, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2024, 9, 10);

        final UUID expectedDefendantId = UUID.randomUUID();

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12,GAEAA_01);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");


        final JsonObject prosecutionReceived =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data.json",
                                ImmutableMap.<String, Object>builder()
                                        .put("case.id", caseId)
                                        .put("case.urn", caseUrn)
                                        .put("offence.id", offenceId)
                                        .put("defendant.id", defendantId)
                                        .put("defendant.dob", dateOfBirth)
                                        .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                        .put("offence.committedDate", offenceCommittedDate)
                                        .put("offence.chargeDate", offenceChargeDate)
                                        .put("offence.endorsable", false)
                                        .put("offence.pressRestrictable", false)
                                        .put("external.id", this.externalId.toString())
                                        .build()))
                        .build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceived, prosecutionReceivedEvent.get().payloadAsJsonObject());

        initiateSjpProsecutionHelper.verifyCaseDetails(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId, dateOfBirth, offenceId, offenceChargeDate, offenceCommittedDate);

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        JsonObject prosecution = prosecutionReceived.getJsonObject("prosecution");
        JsonObject newProsecution = createObjectBuilder(prosecution)
                .remove("externalId")
                .remove("isCivil")
                .remove("isGroupMaster")
                .remove("isGroupMember")
                .build();

        // prosecution-received does not contain enterpriseId & externalId
        final JsonObject expected = createObjectBuilderWithFilter(prosecutionReceived, field -> !FIELD_EXTERNAL_ID.equalsIgnoreCase(field))
                .remove("prosecution")
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .add("prosecution", newProsecution)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp(enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate, offenceCommittedDate, defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);
    }

    @Test
    public void updateOffenceCodeForSjpProsecution() {
        final LocalDate dateOfBirth = LocalDate.of(1999, 9, 10);
        final LocalDate offenceChargeDate = LocalDate.of(2018, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 9, 10);

        final UUID expectedDefendantId = UUID.randomUUID();
        final String offenceCode1 = "GM00001";
        final String offenceCode2 = "GM00003";

        initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, offenceCode1, "GALMT00");

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));

        final JsonObject updateOffenceCodePayload = JsonObjects.createObjectBuilder()
                .add("offenceCode", offenceCode2)
                .build();

        initiateSjpProsecutionHelper.updateOffenceCode(caseId.toString(), updateOffenceCodePayload);

        poll(getCaseDetailsBuilder(caseId.toString()))
                .until(
                        status().is(OK),
                        ResponsePayloadMatcher.payload()
                                .isJson(CoreMatchers.allOf(
                                        withJsonPath("$.caseId", is(caseId.toString())),
                                        withJsonPath("$.defendants[0].offences[0].offenceCode", equalTo("GM00003")))));


    }

    @Test
    public void initiateSjpProsecutionWithoutWarningsForMCC() {
        final LocalDate dateOfBirth = LocalDate.of(1999, 9, 10);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2024, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2024, 9, 10);

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecutionForMCC(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, prosecutorDefendantReference, OFFENCE_CODE_12);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));

        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");

        final JsonObject prosecutionReceived =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-mcc.json",
                                ImmutableMap.<String, Object>builder()
                                        .put("case.id", caseId)
                                        .put("case.urn", caseUrn)
                                        .put("offence.id", offenceId)
                                        .put("defendant.id", defendantId)
                                        .put("defendant.dob", dateOfBirth)
                                        .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                        .put("offence.committedDate", offenceCommittedDate)
                                        .put("offence.chargeDate", offenceChargeDate)
                                        .put("offence.endorsable", false)
                                        .put("offence.pressRestrictable", false)
                                        .build()))
                        .build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceived, prosecutionReceivedEvent.get().payloadAsJsonObject());

        final Optional<JsonEnvelope> publicMCCEvent = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED);
        assertThat(publicMCCEvent.isPresent(), is(true));
        assertThat(publicMCCEvent.get().metadata().name(), equalTo(PUBLIC_PROSECUTIONCASEFILE_MANUAL_CASE_RECEIVED));

        initiateSjpProsecutionHelper.verifyCaseDetailsForMCC(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId, dateOfBirth, offenceId, prosecutorDefendantReference, offenceChargeDate, offenceCommittedDate);

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        JsonObject prosecution = prosecutionReceived.getJsonObject("prosecution");
        JsonObject newProsecution = createObjectBuilder(prosecution)
                .remove("externalId")
                .remove("isCivil")
                .remove("isGroupMaster")
                .remove("isGroupMember")
                .build();

        // prosecution-received does not contain enterpriseId
        final JsonObject expected = createObjectBuilder(prosecutionReceived)
                .remove("prosecution")
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .add("prosecution", newProsecution)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp("stub-data/query.sjp-case-without-warnings-mcc.json",
                enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate,
                offenceCommittedDate, defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);
    }

    @Test
    public void initiateSjpProsecutionWithEndorsableOffences() {
        stubEndorsableOffencesForOffenceCode();
        final LocalDate dateOfBirth = LocalDate.of(1999, 9, 10);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2024, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2024, 9, 10);

        final UUID expectedDefendantId = UUID.randomUUID();

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12, GAEAA_01);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");


        final JsonObject prosecutionReceived =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-endorsable.json",
                                ImmutableMap.<String, Object>builder()
                                        .put("case.id", caseId)
                                        .put("case.urn", caseUrn)
                                        .put("offence.id", offenceId)
                                        .put("defendant.id", defendantId)
                                        .put("defendant.dob", dateOfBirth)
                                        .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                        .put("offence.committedDate", offenceCommittedDate)
                                        .put("offence.chargeDate", offenceChargeDate)
                                        .put("offence.endorsable", true)
                                        .put("offence.pressRestrictable", false)
                                        .build()))
                        .build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceived, prosecutionReceivedEvent.get().payloadAsJsonObject());

        initiateSjpProsecutionHelper.verifyCaseDetails(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId, dateOfBirth, offenceId, offenceChargeDate, offenceCommittedDate);

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        JsonObject prosecution = prosecutionReceived.getJsonObject("prosecution");
        JsonObject newProsecution = createObjectBuilder(prosecution)
                .remove("externalId")
                .remove("isCivil")
                .remove("isGroupMaster")
                .remove("isGroupMember")
                .build();

        // prosecution-received does not contain enterpriseId
        final JsonObject expected = createObjectBuilder(prosecutionReceived)
                .remove("prosecution")
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .add("prosecution", newProsecution)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp(enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate, offenceCommittedDate, defendantId, true, false);
        emitSjpCaseCreatedEvent(caseId);
    }


    @Test
    public void initiateSjpProsecutionWithoutDefendantTitle() {
        final LocalDate dateOfBirth = LocalDate.of(1999, 9, 10);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2024, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2024, 9, 10);

        final UUID expectedDefendantId = UUID.randomUUID();

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecutionWithoutDefendantTitle(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED); //..?
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");


        final JsonObject prosecutionReceived =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-without-defendant-title.json",
                                ImmutableMap.<String, Object>builder()
                                        .put("case.id", caseId)
                                        .put("case.urn", caseUrn)
                                        .put("offence.id", offenceId)
                                        .put("defendant.id", defendantId)
                                        .put("defendant.dob", dateOfBirth)
                                        .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                        .put("offence.committedDate", offenceCommittedDate)
                                        .put("offence.chargeDate", offenceChargeDate)
                                        .build()))
                        .build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceived, prosecutionReceivedEvent.get().payloadAsJsonObject());

        initiateSjpProsecutionHelper.verifyCaseDetailsWithoutDefendantTitle(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId,
                dateOfBirth, offenceId, offenceChargeDate, offenceCommittedDate);

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        // prosecution-received does not contain enterpriseId
        final JsonObject expected = createObjectBuilder(prosecutionReceived)
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp("stub-data/query.sjp-case-without-defendant-title.json", enterpriseIdFromInitiatedEvent,
                dateOfBirth, offenceChargeDate, offenceCommittedDate,
                defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);
    }

    @Test
    public void initiateSjpProsecutionWithoutPostcode() {
        final LocalDate dateOfBirth = LocalDate.of(1999, 9, 10);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2024, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2024, 9, 10);

        final UUID expectedDefendantId = UUID.randomUUID();

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecutionWithoutPostcode(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));

        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");


        final JsonObject prosecutionReceived =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-without-postcode.json",
                                ImmutableMap.<String, Object>builder()
                                        .put("case.id", caseId)
                                        .put("case.urn", caseUrn)
                                        .put("offence.id", offenceId)
                                        .put("defendant.id", defendantId)
                                        .put("defendant.dob", dateOfBirth)
                                        .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                        .put("offence.committedDate", offenceCommittedDate)
                                        .put("offence.chargeDate", offenceChargeDate)
                                        .build()))
                        .build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceived, prosecutionReceivedEvent.get().payloadAsJsonObject());

        initiateSjpProsecutionHelper.verifyCaseDetailsWithoutPostcode(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId,
                dateOfBirth, offenceId, offenceChargeDate, offenceCommittedDate);

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        // prosecution-received does not contain enterpriseId
        final JsonObject expected = createObjectBuilder(prosecutionReceived)
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp("stub-data/query.sjp-case-without-postcode.json",
                enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate, offenceCommittedDate,
                defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);
    }

    @Test
    public void initiateSjpProsecutionWithAsnAndPncIdentifier() {
        final LocalDate dateOfBirth = LocalDate.of(1999, 9, 10);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2024, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2024, 9, 10);

        final UUID expectedDefendantId = UUID.randomUUID();

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecutionWithAsnAndPncIdentifier(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));

        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");


        final JsonObject prosecutionReceived =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-with-asn-and-pnc-identifier.json",
                                ImmutableMap.<String, Object>builder()
                                        .put("case.id", caseId)
                                        .put("case.urn", caseUrn)
                                        .put("offence.id", offenceId)
                                        .put("defendant.id", defendantId)
                                        .put("defendant.dob", dateOfBirth)
                                        .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                        .put("offence.committedDate", offenceCommittedDate)
                                        .put("offence.chargeDate", offenceChargeDate)
                                        .build()))
                        .build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceived, prosecutionReceivedEvent.get().payloadAsJsonObject());

        initiateSjpProsecutionHelper.verifyCaseDetailsWithAsnAndPncIdentifier(initiateSjpProsecutionRequest, caseId, caseUrn, defendantId,
                dateOfBirth, offenceId, offenceChargeDate, offenceCommittedDate);

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));

        final JsonObject prosecutionInitiationEventPayload = prosecutionInitiationEvent.get().payloadAsJsonObject();
        final String enterpriseIdFromInitiatedEvent = prosecutionInitiationEventPayload.getString("enterpriseId");

        // prosecution-received does not contain enterpriseId
        final JsonObject expected = createObjectBuilder(prosecutionReceived)
                .add("enterpriseId", enterpriseIdFromInitiatedEvent)
                .build();

        assertSame(expected, prosecutionInitiationEventPayload);

        verifyCaseCreatedInSjp("stub-data/query.sjp-case-with-asn-and-pnc-identifier.json",
                enterpriseIdFromInitiatedEvent, dateOfBirth, offenceChargeDate,
                offenceCommittedDate, defendantId, false, false);
        emitSjpCaseCreatedEvent(caseId);
    }

    @Test
    public void shouldInitiateSjpProsecution() {
        final LocalDate dateOfBirth = now().plusDays(1); // date of birth in future should make fail the validation
        initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_INVALID, ALCOHOL_LEVEL_METHOD_INVALID);

        final Problem defendantDobProblem = newProblem(DEFENDANT_DOB_IN_FUTURE, DEFENDANT_DOB.getValue(), dateOfBirth.toString());
        final Problem defendantNationalityProblem = newProblem(DEFENDANT_NATIONALITY_INVALID, DEFENDANT_NATIONALITY.getValue(), NATIONALITY_INVALID);
        final Problem alcoholLevelMethodProblem = newProblem(ALCOHOL_DRUG_LEVEL_METHOD_INVALID, OFFENCE_ALCOHOL_LEVEL_METHOD.getValue(), ALCOHOL_LEVEL_METHOD_INVALID);

        final Matcher<ReadContext> prosecutionRejectionPayloadMatcher = getProblemsMatcher(defendantDobProblem, defendantNationalityProblem, alcoholLevelMethodProblem);

        assertRejectionResponse(initiateSjpProsecutionHelper, caseId, prosecutionRejectionPayloadMatcher);
    }

    @Test
    public void initiateInvalidSjpProsecutionWithInvalidOffenceCode() {

        final LocalDate dateOfBirth = now().minusYears(25);
        final LocalDate offenceChargeDate = LocalDate.of(2000, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 9, 10);
        initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, UUID.randomUUID(), "OFCODE13", GAEAA_01);

        final Problem duplicatedProsecutionProblem = newProblem(OFFENCE_CODE_IS_INVALID, OFFENCE_CODE.getValue(), "OFCODE13");

        final Matcher<ReadContext> prosecutionRejectionPayloadMatcher = getProblemsMatcher(duplicatedProsecutionProblem);

        assertRejectionResponse(initiateSjpProsecutionHelper, caseId, prosecutionRejectionPayloadMatcher);
    }

    @Test
    public void validateErrorCaseDetailsOnInvalidOffenceCode() {
        final LocalDate dateOfBirth = now().minusYears(25);
        final LocalDate offenceChargeDate = LocalDate.of(2000, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 9, 10);
        channel = "SPI";
        initiateSjpProsecution(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, UUID.randomUUID(), "OFCODE13", GAEAA_01);
        final String expectedErrorsPayload = readFile("expected/expected_case_details_error_when_invalid_offence_code.json");
        ArrayValueMatcher<Object> arrayValueMatcher = new ArrayValueMatcher<>(new CustomComparator(
                JSONCompareMode.LENIENT,
                new Customization("cases[0].id", (o1, o2) -> true),
                new Customization("cases[0].urn", (o1, o2) -> true),
                new Customization("cases[0].version", (o1, o2) -> true),
                new Customization("cases[0].defendants", (o1, o2) -> true),
                new Customization("cases[0].dateOfBirth", (o1, o2) -> true),
                new Customization("cases[0].errorCaseDetails.caseId", (o1, o2) -> true),
                new Customization("cases[0].errorCaseDetails.defendants[firstName=John].id", (o1, o2) -> true),
                new Customization("cases[0].errorCaseDetails.defendants[firstName=John].offences[description=Duty not paid].id", (o1, o2) -> true)
        ));

        queryAndVerifyCaseErrors(caseId, expectedErrorsPayload, new CustomComparator(NON_EXTENSIBLE,
                new Customization("cases", arrayValueMatcher)));
    }

    @Test
    public void initiateInvalidSjpProsecutionRejectedForMCC() {

        final LocalDate dateOfBirth = now().minusYears(25);
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2000, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 9, 10);
        initiateSjpProsecutionForMCC(initiateSjpProsecutionHelper, dateOfBirth, NATIONALITY_VALID, offenceCommittedDate, offenceChargeDate, prosecutorDefendantReference, "OFCODE13");

        final Problem duplicatedProsecutionProblem = newProblem(OFFENCE_CODE_IS_INVALID, OFFENCE_CODE.getValue(), "OFCODE13");

        final Matcher<ReadContext> prosecutionRejectionPayloadMatcher = getProblemsMatcher(duplicatedProsecutionProblem);

        assertRejectionResponseForMCC(initiateSjpProsecutionHelper, caseId, prosecutionRejectionPayloadMatcher);
    }


    @Test
    public void verifyCaseDetailsWithNonExistentCaseId() {
        final ResponseData responseData = poll(getCaseDetailsBuilder(randomUUID().toString()))
                .until(
                        status().is(Response.Status.NOT_FOUND));

        assertThat(responseData.getStatus(), equalTo(Response.Status.NOT_FOUND));
    }

    @Test
    public void verifyCaseDetailsWithNonExistentProsecutionReference() {
        final ResponseData responseData = poll(getCaseDetailsByProsecutionReferenceIdBuilder(randomUUID().toString()))
                .until(
                        status().is(Response.Status.NOT_FOUND));

        assertThat(responseData.getStatus(), equalTo(Response.Status.NOT_FOUND));
    }

    @Test
    public void initiateInvalidSjpProsecutionWithInvalidOffenceCodeAndMultipleOffences() {

        final LocalDate offenceCommittedDate = LocalDate.of(2018, 1, 1);
        final LocalDate offenceChargeDate = LocalDate.of(2019, 7, 10);
        ImmutableMap<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.id", UUID.randomUUID().toString())
                .put("offence1.code", OFFENCE_CODE_12)
                .put("offence1.committedDate", offenceCommittedDate)
                .put("offence1.chargeDate", offenceChargeDate)
                .put("offence2.code", OFFENCE_CODE_12)
                .put("offence2.committedDate", offenceCommittedDate)
                .put("offence2.chargeDate", offenceChargeDate)
                .put("external.id", externalId)
                .build();
        String template = "stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-multiple-warnings.json";
        final JsonObject initiateSjpProsecutionRequest = readJsonResource(template, params);
        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED_WITH_WARNINGS));

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED_WITH_WARNINGS));

        final JsonObject payload = prosecutionInitiationEvent.get().payloadAsJsonObject();

        assertThat(payload.toString(), allOf(
                hasJsonPath("$.warnings[0].code", equalTo("OFFENCE_OUT_OF_TIME")),
                hasJsonPath("$.warnings[0].values[0].key", equalTo("daysOverdue")),
                hasJsonPath("$.warnings[0].values[0].value", equalTo("282")),
                hasJsonPath("$.warnings[0].values[1].key", equalTo("offence_offenceCode")),
                hasJsonPath("$.warnings[0].values[1].value", equalTo(OFFENCE_CODE_12)),
                hasJsonPath("$.warnings[0].values[2].key", equalTo("offence_offenceSequenceNo")),
                hasJsonPath("$.warnings[0].values[2].value", equalTo("1")),

                hasJsonPath("$.warnings[1].code", equalTo("OFFENCE_OUT_OF_TIME")),
                hasJsonPath("$.warnings[1].values[0].key", equalTo("daysOverdue")),
                hasJsonPath("$.warnings[1].values[0].value", equalTo("282")),
                hasJsonPath("$.warnings[1].values[1].key", equalTo("offence_offenceCode")),
                hasJsonPath("$.warnings[1].values[1].value", equalTo(OFFENCE_CODE_12)),
                hasJsonPath("$.warnings[1].values[2].key", equalTo("offence_offenceSequenceNo")),
                hasJsonPath("$.warnings[1].values[2].value", equalTo("2"))
        ));
    }

    @Test
    public void shouldRaiseProsecutionCaseUnsupportedWhenMultipleDefendantsForSPI() {
        initiateSjpProsecutionSPI(initiateSjpProsecutionHelper, true);

        final HashMap<String, String> keyValues = new HashMap<>();
        keyValues.put("errorMessage", "Multiple Defendants Found");
        keyValues.put("urn", caseUrn);
        keyValues.put("policeSystemId", policeSystemId);
        keyValues.put("channel", "SPI");
        keyValues.put("externalId", externalId.toString());
        assertProsecutionCaseUnsupportedResponseForSPI(initiateSjpProsecutionHelper, keyValues);
    }

    private JsonArray getProblem(final Problem... problems) {
        final JsonArrayBuilder arrayBuilder = JsonObjects.createArrayBuilder();
        for (final Problem problem : problems) {
            final JsonArrayBuilder valuesBuilder = JsonObjects.createArrayBuilder();
            for (final ProblemValue problemValue : problem.getValues()) {
                JsonObjectBuilder problemValueJson = JsonObjects.createObjectBuilder();
                if (problemValue.getId() != null) {
                    problemValueJson.add("id", problemValue.getId());
                }
                problemValueJson.add("key", problemValue.getKey()).add("value", problemValue.getValue());
                valuesBuilder.add(problemValueJson);
            }
            arrayBuilder.add(JsonObjects.createObjectBuilder().add("code", problem.getCode()).add("values", valuesBuilder));
        }
        return arrayBuilder.build();
    }

    private void verifyCaseCreatedInSjp(final String enterpriseId, final LocalDate defendantDob, final LocalDate offenceChargeDate,
                                        final LocalDate offenceCommittedDate, final String defendantId,
                                        final boolean endorsableOffence, final boolean pressRestrictable) {
        verifyCaseCreatedInSjp("stub-data/query.sjp-case.json", enterpriseId, defendantDob, offenceChargeDate, offenceCommittedDate,
                defendantId, endorsableOffence, pressRestrictable);
    }

    private void verifyCaseCreatedInSjp(final String expectedCreateCasePayload,
                                        final String enterpriseId, final LocalDate defendantDob, final LocalDate offenceChargeDate,
                                        final LocalDate offenceCommittedDate, final String defendantId,
                                        final boolean endorsableOffence, final boolean pressRestrictable) {
        final JsonObject createSjpCaseCommand = getSentCreateSjpCaseCommand(caseId).payloadAsJsonObject();

        final JsonObject expectedCreateSjpCaseCommandPayload = readJsonResource(
                expectedCreateCasePayload,
                ImmutableMap.<String, Object>builder()
                        .put("case.id", caseId)
                        .put("case.urn", caseUrn)
                        .put("case.enterpriseId", enterpriseId)
                        .put("defendant.dob", defendantDob)
                        .put("defendant.id", defendantId)
                        .put("offence.chargeDate", offenceChargeDate)
                        .put("offence.committedDate", offenceCommittedDate)
                        .put("offence.endorsable", endorsableOffence)
                        .put("offence.id", createSjpCaseCommand.getJsonObject("defendant").getJsonArray("offences").getJsonObject(0).getString("id"))
                        .put("offence.pressRestrictable", pressRestrictable)
                        .build());

        assertSame(expectedCreateSjpCaseCommandPayload, createSjpCaseCommand);
    }

    private JsonObject initiateSjpProsecution(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality) {
        return initiateSjpProsecution(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("external.id", externalId)
                .put("channel", "CPPI")
                .build());
    }

    private JsonObject initiateSjpProsecutionOrganisation(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality) {
        return initiateSjpProsecutionOrganisation(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("external.id", externalId)
                .build());
    }

    private JsonObject initiateSjpProsecutionSPI(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final boolean hasMultipleDefendants) {
        final String filePath = hasMultipleDefendants ?
                "stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-multiple-defendants-spi.json" :
                "stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-spi.json";

        return initiateSjpProsecutionSPI(initiateSjpProsecutionHelper, filePath,
                ImmutableMap.<String, Object>builder()
                        .put("case.id", caseId)
                        .put("case.urn", caseUrn)
                        .put("external.id", externalId)
                        .put("policeSystemId", policeSystemId)
                        .build());
    }

    private JsonObject initiateSjpProsecution(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality, final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final UUID defendantId, final String offenceCode, final String prosecutingAuthority) {
        return initiateSjpProsecution(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("offence.committedDate", offenceCommittedDate)
                .put("offence.chargeDate", offenceChargeDate)
                .put("prosecutor.prosecutingAuthority", prosecutingAuthority)
                .put("defendant.id", defendantId.toString())
                .put("offence.code", offenceCode)
                .put("external.id", externalId)
                .put("channel", channel)
                .build());
    }

    private JsonObject initiateSjpProsecutionWithoutDefendantTitle(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality, final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final UUID defendantId, final String offenceCode) {
        return initiateSjpProsecutionWithoutDefendantTitle(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("offence.committedDate", offenceCommittedDate)
                .put("offence.chargeDate", offenceChargeDate)
                .put("defendant.id", defendantId.toString())
                .put("offence.code", offenceCode)
                .put("external.id", externalId)
                .build());
    }

    private JsonObject initiateSjpProsecutionWithoutPostcode(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality, final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final UUID defendantId, final String offenceCode) {
        return initiateSjpProsecutionWithoutPostcode(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("offence.committedDate", offenceCommittedDate)
                .put("offence.chargeDate", offenceChargeDate)
                .put("defendant.id", defendantId.toString())
                .put("offence.code", offenceCode)
                .put("external.id", externalId)
                .build());
    }

    private JsonObject initiateSjpProsecutionWithAsnAndPncIdentifier(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality, final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final UUID defendantId, final String offenceCode) {
        return initiateSjpProsecutionWithAsnAndPncIdentifier(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("offence.committedDate", offenceCommittedDate)
                .put("offence.chargeDate", offenceChargeDate)
                .put("defendant.id", defendantId.toString())
                .put("offence.code", offenceCode)
                .put("external.id", externalId)
                .build());
    }

    private JsonObject initiateSjpProsecutionForMCC(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality, final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final String defendantId, final String offenceCode) {
        return initiateSjpProsecutionForMCC(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("offence.committedDate", offenceCommittedDate)
                .put("offence.chargeDate", offenceChargeDate)
                .put("defendant.id", defendantId)
                .put("defendant.prosecutorDefendantReference", defendantId)
                .put("offence.code", offenceCode)
                .build());
    }

    private JsonObject initiateSjpProsecution(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate defendantDob, final String defendantNationality, final String alcoholLevelMethod) {
        return initiateSjpProsecution(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("defendant.dob", defendantDob)
                .put("defendant.nationality", defendantNationality)
                .put("offence.alcoholRelatedOffence.alcoholLevelMethod", alcoholLevelMethod)
                .put("external.id", externalId)
                .put("channel", "CPPI")
                .build());
    }

    private JsonObject initiateSjpProsecution(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution.json", namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }

    private JsonObject initiateSjpProsecutionOrganisation(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-organisation.json", namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }

    private JsonObject initiateSjpProsecutionSPI(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final String filePath, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource(filePath, namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }

    private JsonObject initiateSjpProsecutionWithoutDefendantTitle(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-without-defendant-title.json", namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }

    private JsonObject initiateSjpProsecutionWithoutPostcode(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-without-postcode.json", namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }

    private JsonObject initiateSjpProsecutionWithAsnAndPncIdentifier(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-asn-and-pnc-identifier.json", namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }

    private JsonObject initiateSjpProsecutionForMCC(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-mcc.json", namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }

    private static void assertWarningsResponse(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final UUID caseId, final Matcher<ReadContext> prosecutionRejectionPayloadMatcher) {
        final Matcher matcher = allOf(
                withJsonPath("$.caseId", equalTo(caseId.toString())),
                withJsonPath("$.warnings", prosecutionRejectionPayloadMatcher)
        );

        final Optional<JsonEnvelope> privateEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS);
        assertThat(privateEvent.isPresent(), is(true));
        assertThat(privateEvent.get(), jsonEnvelope(
                metadata().withName(EVENT_SELECTOR_SJP_CASE_CREATED_SUCCESSFULLY_WITH_WARNINGS),
                payload().isJson(matcher)));


        final Optional<JsonEnvelope> publicEvent = initiateSjpProsecutionHelper.retrieveEvent(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS);
        assertThat(publicEvent.isPresent(), is(true));
        assertThat(publicEvent.get(), jsonEnvelope(
                metadata().withName(PUBLIC_PROSECUTIONCASEFILE_PROSECUTION_SUBMISSION_SUCCEEDED_WITH_WARNINGS),
                payload().isJson(matcher)));
    }


    private String generateProsecutorDefendantReference() {
        return randomUUID().toString();
    }

    @Test
    public void initiateSjpProsecutionWithCompanyDefendant() {
        final String prosecutorDefendantReference = generateProsecutorDefendantReference();
        final LocalDate offenceChargeDate = LocalDate.of(2018, 9, 10);
        final LocalDate offenceCommittedDate = LocalDate.of(2018, 9, 10);
        final UUID expectedDefendantId = UUID.randomUUID();

        final JsonObject initiateSjpProsecutionRequest = initiateSjpProsecutionWithCompanyDefendant(initiateSjpProsecutionHelper, offenceCommittedDate, offenceChargeDate, expectedDefendantId, OFFENCE_CODE_12);

        final Optional<JsonEnvelope> prosecutionReceivedEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED);
        assertThat(prosecutionReceivedEvent.isPresent(), is(true));
        final String defendantId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getString("id");
        final String offenceId = JsonObjects.getJsonArray(prosecutionReceivedEvent.get().payloadAsJsonObject(), "prosecution", "defendants").get().getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("offenceId");

        final JsonObject prosecutionReceived =
                JsonObjects.createObjectBuilder(readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-reference-data-with-company-defendant.json",
                                ImmutableMap.<String, Object>builder()
                                        .put("case.id", caseId)
                                        .put("case.urn", caseUrn)
                                        .put("offence.id", offenceId)
                                        .put("defendant.id", defendantId)
                                        .put("defendant.prosecutorDefendantReference", prosecutorDefendantReference)
                                        .put("offence.committedDate", offenceCommittedDate)
                                        .put("offence.chargeDate", offenceChargeDate)
                                        .build()))
                        .build();

        assertThat(prosecutionReceivedEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_RECEIVED));
        assertSame(prosecutionReceived, prosecutionReceivedEvent.get().payloadAsJsonObject());

        final Optional<JsonEnvelope> prosecutionInitiationEvent = initiateSjpProsecutionHelper.retrieveEvent(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED);
        assertThat(prosecutionInitiationEvent.isPresent(), is(true));
        assertThat(prosecutionInitiationEvent.get().metadata().name(), equalTo(EVENT_SELECTOR_SJP_PROSECUTION_INITIATED));
    }

    private JsonObject initiateSjpProsecutionWithCompanyDefendant(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final LocalDate offenceCommittedDate, final LocalDate offenceChargeDate, final UUID defendantId, final String offenceCode) {
        return initiateSjpProsecutionWithCompanyDefendant(initiateSjpProsecutionHelper, ImmutableMap.<String, Object>builder()
                .put("case.id", caseId)
                .put("case.urn", caseUrn)
                .put("offence.committedDate", offenceCommittedDate)
                .put("offence.chargeDate", offenceChargeDate)
                .put("defendant.id", defendantId.toString())
                .put("offence.code", offenceCode)
                .put("external.id", externalId)
                .build());
    }

    private JsonObject initiateSjpProsecutionWithCompanyDefendant(final InitiateSjpProsecutionHelper initiateSjpProsecutionHelper, final Map<String, Object> namedPlaceholders) {
        final JsonObject initiateSjpProsecutionRequest =
                readJsonResource("stub-data/prosecutioncasefile.command.initiate-sjp-prosecution-with-company-defendant.json", namedPlaceholders);

        initiateSjpProsecutionHelper.initiateSjpProsecution(initiateSjpProsecutionRequest);
        return initiateSjpProsecutionRequest;
    }
}
