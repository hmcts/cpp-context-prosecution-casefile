package uk.gov.moj.cpp.prosecution.casefile.service;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CourtRoom;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.OrganisationUnitWithCourtroomReferenceData;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PCF-06 parity characterisation (J17 golden master) for
 * {@link RefDataHelper#asOrganisationUnitWithCourtroomRefData()}.
 *
 * J17 maps the refdata courtroom payload with the framework Jackson ObjectMapper; the PR maps it by
 * hand with JSON-P. Every assertion below encodes the J17 result. The method bodies between the
 * SCENARIOS markers are identical in the J17 and J25 copies; only imports and the harness differ.
 */
class RefDataHelperCourtroomParityTest {

    // ---- SCENARIOS-BEGIN ----

    private static final String FULL_REFDATA_PAYLOAD = """
            {
              "id": "faa91bb2-19cb-384b-bcc1-06d31d12cc67",
              "oucode": "C50EX00",
              "oucodeL1Code": "C",
              "oucodeL1Name": "Crown Courts",
              "oucodeL3Code": "EX",
              "oucodeL3Name": "Exeter Crown Court",
              "oucodeL3WelshName": "Llys Y Goron Exeter",
              "address1": "Exeter Combined Court Centre",
              "address2": "Southernhay Gardens",
              "address3": "Exeter",
              "address4": "Devon",
              "address5": "England",
              "postcode": "EX1 1UH",
              "isWelsh": false,
              "welshAddress1": "W1",
              "welshAddress2": "W2",
              "welshAddress3": "W3",
              "welshAddress4": "W4",
              "welshAddress5": "W5",
              "defaultStartTime": "10:30:00",
              "defaultDurationHrs": "06:00:00",
              "courtLocationCode": "0423",
              "courtId": "423",
              "phone": "0123456789",
              "email": "court@example.org",
              "courtRoom": {
                "id": "be50f1d8-a3ca-3d12-bb8c-66a1a495e141",
                "courtroomId": 905,
                "courtroomName": "Courtroom 01",
                "welshCourtroomName": "Ystafell Llys 01",
                "venueId": 180
              }
            }
            """;

    @Test
    void scenarioD_happyPath_fullPayload_mapsEverySchemaField() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json(FULL_REFDATA_PAYLOAD));

        assertThat(result, is(notNullValue()));
        assertThat(result.getId(), is("faa91bb2-19cb-384b-bcc1-06d31d12cc67"));
        assertThat(result.getOucode(), is("C50EX00"));
        assertThat(result.getOucodeL1Code(), is("C"));
        assertThat(result.getOucodeL1Name(), is("Crown Courts"));
        assertThat(result.getOucodeL3Code(), is("EX"));
        assertThat(result.getOucodeL3Name(), is("Exeter Crown Court"));
        assertThat(result.getOucodeL3WelshName(), is("Llys Y Goron Exeter"));
        assertThat(result.getAddress1(), is("Exeter Combined Court Centre"));
        assertThat(result.getAddress2(), is("Southernhay Gardens"));
        assertThat(result.getAddress3(), is("Exeter"));
        assertThat(result.getAddress4(), is("Devon"));
        assertThat(result.getAddress5(), is("England"));
        assertThat(result.getPostcode(), is("EX1 1UH"));
        assertThat(result.getIsWelsh(), is(false));
        assertThat(result.getWelshAddress1(), is("W1"));
        assertThat(result.getWelshAddress2(), is("W2"));
        assertThat(result.getWelshAddress3(), is("W3"));
        assertThat(result.getWelshAddress4(), is("W4"));
        assertThat(result.getWelshAddress5(), is("W5"));
        assertThat(result.getDefaultStartTime(), is("10:30:00"));
        assertThat(result.getDefaultDurationHrs(), is("06:00:00"));
        assertThat(result.getCourtRoom().getId(), is("be50f1d8-a3ca-3d12-bb8c-66a1a495e141"));
        assertThat(result.getCourtRoom().getCourtroomId(), is(905));
        assertThat(result.getCourtRoom().getCourtroomName(), is("Courtroom 01"));
    }

    @Test
    void scenarioA_fullPayload_carriesTopLevelNonSchemaFieldsInAdditionalProperties() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json(FULL_REFDATA_PAYLOAD));

        final Map<String, Object> expected = new HashMap<>();
        expected.put("courtLocationCode", "0423");
        expected.put("courtId", "423");
        expected.put("phone", "0123456789");
        expected.put("email", "court@example.org");
        assertThat(result.getAdditionalProperties(), is(expected));
    }

    @Test
    void scenarioA_fullPayload_carriesCourtRoomNonSchemaFieldsInAdditionalProperties() {
        final CourtRoom courtRoom = map(json(FULL_REFDATA_PAYLOAD)).getCourtRoom();

        final Map<String, Object> expected = new HashMap<>();
        expected.put("welshCourtroomName", "Ystafell Llys 01");
        expected.put("venueId", 180);
        assertThat(courtRoom.getAdditionalProperties(), is(expected));
    }

    @Test
    void scenarioA_itStubPayloads_nonSchemaKeysCarriedThrough() throws IOException {
        final OrganisationUnitWithCourtroomReferenceData noCourtroom = map(json(stub("referencedata.ou-no-courtroom.json")));
        assertThat(keysOf(noCourtroom.getAdditionalProperties()), is(keys("defaultDuration")));
        assertThat(noCourtroom.getCourtRoom(), is(nullValue()));

        final OrganisationUnitWithCourtroomReferenceData oneCourtroom = map(json(stub("referencedata.ou-one-courtroom.json")));
        assertThat(keysOf(oneCourtroom.getAdditionalProperties()), is(nullValue()));
        assertThat(keysOf(oneCourtroom.getCourtRoom().getAdditionalProperties()), is(nullValue()));

        final OrganisationUnitWithCourtroomReferenceData mags = map(json(stub("referencedata.ou-one-courtroom-mags.json")));
        assertThat(keysOf(mags.getAdditionalProperties()), is(keys("defaultDuration")));
        assertThat(keysOf(mags.getCourtRoom().getAdditionalProperties()), is(nullValue()));

        final OrganisationUnitWithCourtroomReferenceData submitApplication = map(json(stub("referencedata.ou-one-courtroom-submit-application.json")));
        assertThat(keysOf(submitApplication.getAdditionalProperties()), is(keys("defaultDuration")));
        assertThat(keysOf(submitApplication.getCourtRoom().getAdditionalProperties()), is(nullValue()));

        final OrganisationUnitWithCourtroomReferenceData crown = map(json(stub("referencedata.ou-one-courtroom-crown.json")));
        assertThat(keysOf(crown.getAdditionalProperties()), is(keys("courtLocationCode", "courtId")));
        assertThat(keysOf(crown.getCourtRoom().getAdditionalProperties()), is(keys("venueId")));
    }

    @Test
    void scenarioB_stringIsWelsh_isCoercedToBoolean() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": "C50EX00", "isWelsh": "true"}
                """));

        assertThat(result.getIsWelsh(), is(true));
    }

    @Test
    void scenarioB_stringCourtroomId_isCoercedToInteger() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": "C50EX00", "courtRoom": {"id": "r", "courtroomId": "905", "courtroomName": "Courtroom 01"}}
                """));

        assertThat(result.getCourtRoom().getCourtroomId(), is(905));
    }

    @Test
    void scenarioB_numericDefaultDurationHrs_isCoercedToString() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": "C50EX00", "defaultDurationHrs": 6}
                """));

        assertThat(result.getDefaultDurationHrs(), is("6"));
    }

    @Test
    void scenarioB_numericOucode_isCoercedToString() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": 123}
                """));

        assertThat(result.getOucode(), is("123"));
    }

    @Test
    void scenarioC_nonNumericCourtroomId_returnsNullSoCallerGetsEmpty() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": "C50EX00", "courtRoom": {"id": "r", "courtroomId": "abc"}}
                """));

        assertThat(result, is(nullValue()));
    }

    @Test
    void scenarioC_courtRoomNotAnObject_returnsNullSoCallerGetsEmpty() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": "C50EX00", "courtRoom": "Courtroom 01"}
                """));

        assertThat(result, is(nullValue()));
    }

    @Test
    void scenarioC_nonBooleanIsWelsh_returnsNullSoCallerGetsEmpty() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": "C50EX00", "isWelsh": "yes"}
                """));

        assertThat(result, is(nullValue()));
    }

    @Test
    void scenarioC_jsonNullValue_returnsNull() {
        final OrganisationUnitWithCourtroomReferenceData result = map(JsonValue.NULL);

        assertThat(result, is(nullValue()));
    }

    @Test
    void scenarioC_emptyObject_mapsToAllNullFields() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("{}"));

        assertThat(result, is(notNullValue()));
        assertThat(result.getId(), is(nullValue()));
        assertThat(result.getOucode(), is(nullValue()));
        assertThat(result.getIsWelsh(), is(nullValue()));
        assertThat(result.getCourtRoom(), is(nullValue()));
        assertThat(result.getAdditionalProperties(), is(nullValue()));
    }

    @Test
    void scenarioC_explicitJsonNulls_mapToNullFields() {
        final OrganisationUnitWithCourtroomReferenceData result = map(json("""
                {"id": "x", "oucode": "C50EX00", "address3": null, "isWelsh": null, "courtRoom": null}
                """));

        assertThat(result, is(notNullValue()));
        assertThat(result.getAddress3(), is(nullValue()));
        assertThat(result.getIsWelsh(), is(nullValue()));
        assertThat(result.getCourtRoom(), is(nullValue()));
    }

    @Test
    void control_frameworkJacksonMapper_carriesNonSchemaFieldsAndCoerces() throws IOException {
        final OrganisationUnitWithCourtroomReferenceData full = jackson(FULL_REFDATA_PAYLOAD);
        assertThat(keysOf(full.getAdditionalProperties()), is(keys("courtLocationCode", "courtId", "phone", "email")));
        assertThat(keysOf(full.getCourtRoom().getAdditionalProperties()), is(keys("welshCourtroomName", "venueId")));
        assertThat(jackson("{\"isWelsh\": \"true\"}").getIsWelsh(), is(true));
        assertThat(jackson("{\"courtRoom\": {\"courtroomId\": \"905\"}}").getCourtRoom().getCourtroomId(), is(905));
        assertThat(jackson("{\"defaultDurationHrs\": 6}").getDefaultDurationHrs(), is("6"));
        assertThat(jackson("{}").getAdditionalProperties(), is(nullValue()));
    }

    @Test
    void control_frameworkJacksonMapper_rejectsNonNumericCourtroomIdWithIOException() {
        assertThrows(IOException.class, () -> jackson("{\"courtRoom\": {\"courtroomId\": \"abc\"}}"));
    }

    // ---- SCENARIOS-END ----

    private static OrganisationUnitWithCourtroomReferenceData map(final JsonValue jsonValue) {
        return RefDataHelper.asOrganisationUnitWithCourtroomRefData().apply(jsonValue);
    }

    private static OrganisationUnitWithCourtroomReferenceData jackson(final String text) throws IOException {
        return new ObjectMapperProducer().objectMapper().readValue(text, OrganisationUnitWithCourtroomReferenceData.class);
    }

    private static JsonObject json(final String text) {
        try (JsonReader reader = Json.createReader(new StringReader(text))) {
            return reader.readObject();
        }
    }

    private static String stub(final String name) throws IOException {
        return new String(Files.readAllBytes(Paths.get("..", "prosecutioncasefile-integration-test", "src", "test", "resources", "stub-data", name)), UTF_8);
    }

    private static Set<String> keysOf(final Map<String, Object> additionalProperties) {
        return additionalProperties == null ? null : new TreeSet<>(additionalProperties.keySet());
    }

    private static Set<String> keys(final String... names) {
        final Set<String> set = new TreeSet<>();
        for (final String name : names) {
            set.add(name);
        }
        return set;
    }
}
