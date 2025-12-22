package uk.gov.moj.cpp.prosecution.casefile.validation.rules.forms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MatchedDefendant;
import uk.gov.moj.cpp.prosecution.casefile.validation.ValidationError;
import uk.gov.moj.cpp.prosecution.casefile.validation.utils.FileUtil;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AsnValidationRuleTest {

    private String matchId1 = UUID.randomUUID().toString();
    private String defendantId1 = UUID.randomUUID().toString();
    private String defendantId2 = UUID.randomUUID().toString();
    private String defendantId3 = UUID.randomUUID().toString();

    private final AsnValidationRule asnValidationRule = new AsnValidationRule();
    private JsonObject prosecutionCaseFileCase;

    @BeforeEach
    public void setup() {
        prosecutionCaseFileCase = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/prosecutioncasefile.case.json")
                .replaceAll("DEFENDANT_ID_1", defendantId1)
                .replaceAll("DEFENDANT_ID_2", defendantId2)
                .replaceAll("DEFENDANT_ID_3", defendantId3));
    }

    @Test
    public void shouldReturnJsonObjectWhen_AsnMatched() {
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_asn.json")
                .replaceAll("ASN","ASN_1")
                .replaceAll("MATCHING_ID1", matchId1));

        final MatchedDefendant matchedDefendant = asnValidationRule.validate(defendantData, prosecutionCaseFileCase);
        assertThat(matchedDefendant.getDefendantId(), notNullValue());
        assertThat(matchedDefendant.getDefendantId().toString(), is(defendantId1));
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), nullValue());
    }

    @Test
    public void shouldReturnJsonObjectWhen_AsnNotMatched() {
        final String asnValue = "invalidAsn";
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_asn.json")
                .replaceAll("ASN",asnValue)
                .replaceAll("MATCHING_ID1", matchId1));

        final MatchedDefendant matchedDefendant = asnValidationRule.validate(defendantData, prosecutionCaseFileCase);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getKey(), is(FormConstant.ASN));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getValue(), is(asnValue));
    }

    @Test
    public void shouldReturnJsonObjectWhen_AsnNotPresent() {
        final String asnValue = "invalidAsn";
        final JsonObject defendantData = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/defendantdata_no_data_present.json")
                .replaceAll("ASN",asnValue)
                .replaceAll("MATCHING_ID1", matchId1));

        final MatchedDefendant matchedDefendant = asnValidationRule.validate(defendantData, prosecutionCaseFileCase);
        assertThat(matchedDefendant.getDefendantId(), nullValue());
        assertThat(matchedDefendant.getMatchingId().toString(), is(matchId1));
        assertThat(matchedDefendant.getProblems(), notNullValue());
        assertThat(matchedDefendant.getProblems(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getCode(), is(ValidationError.INVALID_DEFENDANTS_PROVIDED.getCode()));
        assertThat(matchedDefendant.getProblems().get(0).getValues(), hasSize(1));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getKey(), is(FormConstant.ASN));
        assertThat(matchedDefendant.getProblems().get(0).getValues().get(0).getValue(), isEmptyString());
    }

}

