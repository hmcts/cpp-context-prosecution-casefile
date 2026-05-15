package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cpp.prosecution.casefile.validation.utils.FileUtil;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NameAndDOBValidationRuleTest {

    private String matchId1 = UUID.randomUUID().toString();
    private String defendantId1 = UUID.randomUUID().toString();
    private String defendantId2 = UUID.randomUUID().toString();
    private String defendantId3 = UUID.randomUUID().toString();
    private static final UUID DEFENDANT_ID2_VALUE = randomUUID();
    public static final String CPS_DEFENDANT_ID = "cpsDefendantId";
    public static final String DEFENDANT_ID = "defendantId";

    private final NameAndDOBValidationRule nameAndDOBValidationRule = new NameAndDOBValidationRule();

    private JsonObject prosecutionCaseFileCase;
    private JsonObject prosecutionCaseFileCaseWithCpsDefendantIds;

    @BeforeEach
    public void setup() {
        prosecutionCaseFileCase = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantId1)
                .replaceAll("DEFENDANT_ID_2", defendantId2)
                .replaceAll("DEFENDANT_ID_3", defendantId3));
        prosecutionCaseFileCaseWithCpsDefendantIds = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/prosecutioncasefile.case_with_cpsdefendantids.json")
                .replaceAll("DEFENDANT_ID_1", defendantId1)
                .replaceAll("DEFENDANT_ID_2", defendantId2)
                .replaceAll("DEFENDANT_ID_3", defendantId3));
    }

    @Test
    public void shouldReturnJsonObjectWhen_AllDefendantDataMatched_WhenCpsDefendantDoNotHaveCpsDefendantId_CaseDefendantDoNoHaveCpsDefendantIds() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_defendantdata.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_1")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId2)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_2")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId1));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
        assertThat(matchedDefendant.getCpsDefendantId(), nullValue());

    }


    @Test
    public void shouldReturnJsonObjectWhen_AllDefendantDataMatched_WhenCpsDefendantHaveCpsDefendantId_CaseDefendantHaveDifferentCpsDefendantIds() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_defendantdata_withcpsdefendantid.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_1")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId2)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_2")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCaseWithCpsDefendantIds)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getCpsDefendantId(), nullValue());
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getKey(), is(CPS_DEFENDANT_ID));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getValue(), is("CPS_DEFENDANT_ID_0"));
    }

    @Test
    public void shouldReturnJsonObjectWhen_AllDefendantDataMatched_WhenCpsDefendantHaveCpsDefendantId_CaseDefendantSameCpsDefendantIds() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_defendantdata_withcpsdefendantid.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_0")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId2)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_2")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCaseWithCpsDefendantIds)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId1));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
        assertThat(matchedDefendant.getCpsDefendantId(), nullValue());
    }

    @Test
    public void shouldReturnJsonObjectWhen_AllDefendantDataMatched_WhenCpsDefendantHaveCpsDefendantId_CaseDefendantDoNoHaveCpsDefendantIds() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_defendantdata_withcpsdefendantid.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId3)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_3")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId1));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
        assertThat(matchedDefendant.getCpsDefendantId(), is("CPS_DEFENDANT_ID_0"));

    }

    @Test
    public void shouldReturnJsonObjectWhen_AllDefendantDataMatched_WhenCpsDefendantHaveCpsDefendantId_CaseDefendantHaveCpsDefendantIds() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_defendantdata.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCaseWithCpsDefendantIds)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId1));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
        assertThat(matchedDefendant.getCpsDefendantId(), nullValue());

    }

    @Test
    public void shouldReturnJsonObjectWhen_AllOrganisationDataMatched_WhenCpsDefendantDoNotHaveCpsDefendantId_CaseDefendantDoNoHaveCpsDefendantIds() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_organisation_data.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId2));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
        assertThat(matchedDefendant.getCpsDefendantId(), nullValue());

    }

    @Test
    public void shouldReturnJsonObjectWhen_NoOrganisationMatched() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_no_organisation_matching.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getKey(), is(FormConstant.ORGANISATION_NAME));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getValue(), is("unknown"));
    }

    @Test
    public void shouldReturnJsonObjectWhen_NoDefendantDataMatched() {
        final String unMatchingForName = "unMatchingForName";
        final String unMatchingSurName = "unMatchingSurName";
        final String unMatchingDob = "1990-12-01";
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_no_defendantdata_matching.json")
                .replaceAll("MATCHING_ID1", matchId1)
                .replaceAll("FOR_NAME", unMatchingForName)
                .replaceAll("SUR_NAME", unMatchingSurName)
                .replaceAll("DATE_OF_BIRTH", unMatchingDob));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(3));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getKey(), anyOf(is(FormConstant.FORENAME),is(FormConstant.SURNAME),is(FormConstant.DATE_OF_BIRTH)));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getValue(), anyOf(is(unMatchingForName),is(unMatchingSurName),is(unMatchingDob)));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(1).getKey(), anyOf(is(FormConstant.FORENAME),is(FormConstant.SURNAME),is(FormConstant.DATE_OF_BIRTH)));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(1).getValue(), anyOf(is(unMatchingForName),is(unMatchingSurName),is(unMatchingDob)));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(2).getKey(), anyOf(is(FormConstant.FORENAME),is(FormConstant.SURNAME),is(FormConstant.DATE_OF_BIRTH)));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(2).getValue(), anyOf(is(unMatchingForName),is(unMatchingSurName),is(unMatchingDob)));
    }

    @Test
    public void shouldReturnJsonObjectWhen_NoDataPresent() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_no_data_present.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(3));
    }

    @Test
    public void shouldReturnJsonObjectWhen_OnlyDobPresent() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_dob_only.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(2));
    }

    @Test
    public void shouldReturnJsonObjectWhen_OnlyForNamePresent() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_first_name_only.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(2));
    }

    @Test
    public void shouldReturnJsonObjectWhen_OnlySurNamePresent() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_last_name_only.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(2));
    }

    @Test
    public void shouldReturnJsonObjectWhen_OnlyForeNameSurNamePresent() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_forename_lastname_only.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(1));
    }


    @Test
    public void shouldReturnJsonObjectWhen_OnlyForeNameDOBPresent() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_forename_dob_only.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(1));
    }


    @Test
    public void shouldReturnJsonObjectWhen_OnlyLastNameDOBPresent() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_lastname_dob_only.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(1));
    }

    @Test
    public void shouldReturnJsonObjectWhen_OrgNameMatching_caseInsensitive() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_organisation_data1.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId2));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
        assertThat(matchedDefendant.getCpsDefendantId(), nullValue());
    }

    @Test
    public void shouldReturnJsonObjectWhen_defendantName_caseInsentive() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_all_defendantName.json")
                .replaceAll("MATCHING_ID1", matchId1));

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId1)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_1")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, defendantId2)
                        .add(CPS_DEFENDANT_ID, "CPS_DEFENDANT_ID_2")
                        .build())
                .build();

        JsonObject cpsDefendantDetailRuleInput = JsonObjects.createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("prosecutionCase", prosecutionCaseFileCase)
                .build();

        final MatchedDefendant matchedDefendant = nameAndDOBValidationRule.validate(defendantData, cpsDefendantDetailRuleInput);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId1));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
        assertThat(matchedDefendant.getCpsDefendantId(), nullValue());

    }
}

