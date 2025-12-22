package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.prosecution.casefile.domain.ProsecutionWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.*;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult.VALID;

class CaseNewhearingListDefendantsValidationRuleTest {

    private CaseNewhearingListDefendantsValidationRule rule;

    @BeforeEach
    void setUp() {
        rule = new CaseNewhearingListDefendantsValidationRule();
    }

    @Test
    void shouldReturnValidWhenChannelIsNotMCC() {
        Prosecution prosecution = Prosecution.prosecution().withChannel(Channel.CIVIL).build();

        ProsecutionWithReferenceData data = new ProsecutionWithReferenceData(prosecution, null);

        ValidationResult result = rule.validate(data, null);
        assertEquals(VALID, result);
    }

    @Test
    void shouldReturnValidWhenChannelIsMCCButListNewHearingIsNull() {
        Prosecution prosecution = Prosecution.prosecution().withChannel(Channel.MCC).build();

        ProsecutionWithReferenceData data = new ProsecutionWithReferenceData(prosecution, null);

        ValidationResult result = rule.validate(data, null);
        assertEquals(VALID, result);
    }

    @Test
    void shouldReturnValidWhenMCCAndListNewHearingHasSameNumberOfDefendants() {

        HearingRequest newHearing = HearingRequest.hearingRequest().withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest().build())). build();
        Prosecution prosecution = Prosecution.prosecution().withChannel(Channel.MCC).withDefendants(List.of(Defendant.defendant().build())).withListNewHearing(newHearing).build();

        ProsecutionWithReferenceData data = new ProsecutionWithReferenceData(prosecution, null);

        ValidationResult result = rule.validate(data, null);
        assertEquals(VALID, result);
    }

    @Test
    void shouldReturnInvalidWhenMCCAndListNewHearingHasDifferentNumberOfDefendants() {
        HearingRequest newHearing = HearingRequest.hearingRequest().withListDefendantRequests(List.of(ListDefendantRequest.listDefendantRequest().build())). build();
        Prosecution prosecution = Prosecution.prosecution().withChannel(Channel.MCC).withDefendants(List.of(Defendant.defendant().build(), Defendant.defendant().build())).withListNewHearing(newHearing).build();

        ProsecutionWithReferenceData data = new ProsecutionWithReferenceData(prosecution, null);

        ValidationResult result = rule.validate(data, null);

        assertNotEquals(VALID, result);
        assertTrue(result.problems().get(0).
                getCode().equals("CASE_LIST_NEW_LISTING_HEARING")
                        && result.problems().get(0).getValues().get(0).getKey().equals("listDefendantRequests"));
    }

}
