package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError.INVALID_DEFENDANTS_PROVIDED;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;

import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CpsDefendantIdValidationRuleTest {

    private static final UUID MATCHING_ID_VALUE = randomUUID();
    private static final UUID DEFENDANT_ID1_VALUE = randomUUID();
    private static final UUID DEFENDANT_ID2_VALUE = randomUUID();
    public static final String CPS_DEFENDANT_ID = "cpsDefendantId";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String MATCHING_ID = "matchingId";

    @Mock
    private Instance<ValidationRule> validatorRules;

    @InjectMocks
    private final CpsDefendantIdValidationRule cpsDefendantIdValidationRule = new CpsDefendantIdValidationRule();


    @Test
    public void shouldReturnJsonObjectWhenCpsDefendantIdMatched_AllCaseDefendantsHaveCpsDefendantIds() {
        final JsonObject cpsDefendant = createObjectBuilder()
                .add(MATCHING_ID, MATCHING_ID_VALUE.toString())
                .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                .build();

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID1_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();
        final JsonObject ruleInput = createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("areAllDefendantsHaveCpsIds", true)
                .build();

        final MatchedDefendant matchedDefendant = cpsDefendantIdValidationRule.validate(cpsDefendant, ruleInput);

        assertThat(matchedDefendant.getDefendantId(), is(DEFENDANT_ID1_VALUE));
        assertThat(matchedDefendant.getMatchingId(), is(MATCHING_ID_VALUE));
        assertThat(matchedDefendant.getIsContinueMatching(), is(false));
        assertThat(matchedDefendant.getProblems(), nullValue());
    }

    @Test
    public void shouldReturnJsonObjectWhenCpsDefendantIdMatched_AllCaseDefendantsDoNotHaveCpsDefendantIds() {
        final JsonObject cpsDefendant = createObjectBuilder()
                .add(MATCHING_ID, MATCHING_ID_VALUE.toString())
                .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                .build();

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID1_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();
        final JsonObject ruleInput = createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("areAllDefendantsHaveCpsIds", false)
                .build();

        final MatchedDefendant matchedDefendant = cpsDefendantIdValidationRule.validate(cpsDefendant, ruleInput);

        assertThat(matchedDefendant.getDefendantId(), is(DEFENDANT_ID1_VALUE));
        assertThat(matchedDefendant.getMatchingId(), is(MATCHING_ID_VALUE));
        assertThat(matchedDefendant.getIsContinueMatching(), is(false));
        assertThat(matchedDefendant.getProblems(), nullValue());
    }

    @Test
    public void shouldReturnProblemWhenCpsDefendantIdIsNotMatched_AllCaseDefendantsHaveCpsDefendantIds() {
        final JsonObject cpsDefendant = createObjectBuilder()
                .add(MATCHING_ID, MATCHING_ID_VALUE.toString())
                .add(CPS_DEFENDANT_ID, "thisIsNotMatchedId")
                .build();

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID1_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        final JsonObject ruleInput = createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("areAllDefendantsHaveCpsIds", true)
                .build();

        final MatchedDefendant matchedDefendant = cpsDefendantIdValidationRule.validate(cpsDefendant, ruleInput);

        assertThat(matchedDefendant.getDefendantId(), is(nullValue()));
        assertThat(matchedDefendant.getMatchingId(), is(nullValue()));
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getIsContinueMatching(), is(false));

        final Problem problem = matchedDefendant.getProblems().get(0);

        assertThat(problem.getCode(), is(INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(problem.getValues(), hasSize(1));
        assertThat(problem.getValues().get(0).getKey(), is(CPS_DEFENDANT_ID));
        assertThat(problem.getValues().get(0).getValue(), is("thisIsNotMatchedId"));
    }

    @Test
    public void shouldReturnProblemWhenCpsDefendantIdIsNotMatched_AllCaseDefendantsDoNotHaveCpsDefendantIds() {
        final JsonObject cpsDefendant = createObjectBuilder()
                .add(MATCHING_ID, MATCHING_ID_VALUE.toString())
                .add(CPS_DEFENDANT_ID, "thisIsNotMatchedId")
                .build();

        final JsonArray defendantIdsAsJsonArrayFromProsecutionCase = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID1_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsACpsDefendantId")
                        .build())
                .add(createObjectBuilder()
                        .add(DEFENDANT_ID, DEFENDANT_ID2_VALUE.toString())
                        .add(CPS_DEFENDANT_ID, "thisIsAnotherCpsDefendantId")
                        .build())
                .build();

        final JsonObject ruleInput = createObjectBuilder()
                .add("cpsDefendantIdList", defendantIdsAsJsonArrayFromProsecutionCase)
                .add("areAllDefendantsHaveCpsIds", false)
                .build();

        final MatchedDefendant matchedDefendant = cpsDefendantIdValidationRule.validate(cpsDefendant, ruleInput);

        assertThat(matchedDefendant.getDefendantId(), is(nullValue()));
        assertThat(matchedDefendant.getMatchingId(), is(nullValue()));
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getIsContinueMatching(), is(true));

        final Problem problem = matchedDefendant.getProblems().get(0);

        assertThat(problem.getCode(), is(INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(problem.getValues(), hasSize(1));
        assertThat(problem.getValues().get(0).getKey(), is(CPS_DEFENDANT_ID));
        assertThat(problem.getValues().get(0).getValue(), is("thisIsNotMatchedId"));
    }

}

