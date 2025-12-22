package uk.gov.moj.cpp.prosecution.casefile.it;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.prosecution.casefile.helper.CaseFileQueryHelper.getCaseForCitizenWithMandatoryQueryParams;
import static uk.gov.moj.cpp.prosecution.casefile.helper.CaseFileQueryHelper.getCaseForCitizenWithNoMandatoryQueryParams;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubDefendantHearingDays;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubGetProsecutionCase;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubGetProsecutionCaseLegalEntity;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ProgressionStub.stubSearchCases;
import static uk.gov.moj.cpp.prosecution.casefile.stub.ReferenceDataOffencesStub.stubOffencesForOffenceCodeList;
import static uk.gov.moj.cpp.prosecution.casefile.stub.SjpStub.stubCaseByUrnPostcode;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseFileQueryIT extends BaseIT {

    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String caseUrn;
    private String caseId;
    private String defendantId;
    private String postcode;
    private final String dob = "1990-01-02";

    @BeforeEach
    public void setup() {
        caseUrn = randomAlphanumeric(10);
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        postcode = randomAlphanumeric(8);
    }

    @Test
    public void shouldReturnBadRequestWhenNoCaseUrnAndPostcodeAndDefendantTypeWhenQueryCaseForCitizen() {
        getCaseForCitizenWithNoMandatoryQueryParams(USER_ID, status().is(BAD_REQUEST));
    }

    @Test
    public void shouldReturnCaseDefendantWhenFoundMatchInSjp() {
        stubCaseByUrnPostcode(caseUrn, postcode, dob);
        final String response = getCaseForCitizenWithMandatoryQueryParams(USER_ID, caseUrn, postcode, "PERSON", dob, status().is(OK));

        final JsonObject jsonResponse = stringToJsonObjectConverter.convert(response);
        assertThat(jsonResponse.getString("urn"), is(caseUrn));
    }

    @Test
    public void shouldReturnCaseDefendantPersonFromProgressionWhenNoMatchInSjp() {
        stubSearchCases(caseUrn, caseId);
        stubGetProsecutionCase(caseId, caseUrn, postcode, dob);
        stubDefendantHearingDays(caseId, defendantId);

        final String response = getCaseForCitizenWithMandatoryQueryParams(USER_ID, caseUrn, postcode, "PERSON", dob, status().is(OK));

        final JsonObject jsonResponse = stringToJsonObjectConverter.convert(response);
        assertThat(jsonResponse.getString("urn"), is(caseUrn));
        assertThat(jsonResponse.getString("id"), is(caseId));
        assertThat(jsonResponse.getJsonObject("defendant").getJsonObject("personalDetails").getJsonObject("address").getString("postcode"), is(postcode));
    }

    @Test
    public void shouldReturnCaseDefendantWhenFoundMatchInSjpWhenPCQVisited() {
        stubCaseByUrnPostcode(caseUrn, postcode, dob);
        final String response = getCaseForCitizenWithMandatoryQueryParams(USER_ID, caseUrn, postcode, "PERSON", dob, status().is(OK));
        final JsonObject jsonResponse = stringToJsonObjectConverter.convert(response);
        assertThat(jsonResponse.getString("urn"), is(caseUrn));
        assertThat(jsonResponse.getString("type"), is("SJP"));
        assertThat(jsonResponse.getString("initiationCode"), is("J"));
        assertThat(jsonResponse.getJsonObject("defendant").getString("pcqId"), is("883a6942-62f0-45cc-8b1a-202ce9d0aadd"));
    }

    @Test
    public void shouldReturnCaseDefendantPersonFromProgressionWhenNoMatchInSjpWhenNotPCQVisited() {
        stubSearchCases(caseUrn, caseId);
        stubGetProsecutionCase(caseId, caseUrn, postcode, dob);
        stubDefendantHearingDays(caseId, defendantId);

        final String response = getCaseForCitizenWithMandatoryQueryParams(USER_ID, caseUrn, postcode, "PERSON", dob, status().is(OK));

        final JsonObject jsonResponse = stringToJsonObjectConverter.convert(response);
        assertThat(jsonResponse.getString("urn"), is(caseUrn));
        assertThat(jsonResponse.getString("id"), is(caseId));
        assertThat(jsonResponse.getJsonObject("defendant").getJsonObject("personalDetails").getJsonObject("address").getString("postcode"), is(postcode));
        assertThat(jsonResponse.getJsonObject("defendant").get("pcqId"), is(nullValue()));
    }

    @Test
    public void shouldReturnCaseDefendantLegalEntityFromProgression() {
        stubSearchCases(caseUrn, caseId);
        stubGetProsecutionCaseLegalEntity(caseId, caseUrn, postcode);
        stubDefendantHearingDays(caseId, defendantId);
        stubOffencesForOffenceCodeList();

        final String response = getCaseForCitizenWithMandatoryQueryParams(USER_ID, caseUrn, postcode, "LEGAL_ENTITY", dob, status().is(OK));

        final JsonObject jsonResponse = stringToJsonObjectConverter.convert(response);
        assertThat(jsonResponse.getString("urn"), is(caseUrn));
        assertThat(jsonResponse.getString("id"), is(caseId));
        assertThat(jsonResponse.getJsonObject("defendant").getJsonObject("legalEntityDefendant").getJsonObject("address").getString("postcode"), is(postcode));
        assertThat(jsonResponse.getJsonObject("defendant").get("pcqId"), is(nullValue()));
    }

}
