package uk.gov.moj.cpp.prosecution.casefile.validation.rules.defendant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.HEARING_TYPE_CODE_INVALID;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingType;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.HearingTypes;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.prosecution.casefile.validation.rules.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingTypeCodeValidationRuleTest {

    private static final String HEARING_TYPE_CODE = "FPTP";
    private static final String HEARING_DESCRIPTION = "Further Plea & Trial Preparation";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @Test
    public void shouldReturnEmptyListWhenCaseHearingTypeCodeIsValid() {

        final HearingTypes hearingTypes = getMockHearingTypesReferenceData(HEARING_TYPE_CODE, HEARING_DESCRIPTION);
        final InitialHearing initialHearing = new InitialHearing(null,null, null, null, null,null,null,HEARING_TYPE_CODE, null, null, null);

        when(defendantWithReferenceData.getDefendant().getInitialHearing()).thenReturn(initialHearing);
        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(hearingTypes);

        final ValidationResult optionalProblem = new HearingTypeCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        assertThat(optionalProblem.isValid(), is(true));
    }

    @Test
    public void shouldReturnProblemWhenCaseHearingTypeCodeIsInvalid() {

        final String invalidPoliceForceCode = "X";
        final HearingTypes hearingTypes = getMockHearingTypesReferenceData(invalidPoliceForceCode, "invalid hearing type");
        final InitialHearing initialHearing = new InitialHearing(null,null,null,null,null,null,null, invalidPoliceForceCode, null, null, null);

        when(defendantWithReferenceData.getDefendant().getInitialHearing()).thenReturn(initialHearing);
        when(referenceDataQueryService.retrieveHearingTypes()).thenReturn(hearingTypes);

        final Optional<Problem> optionalProblem = new HearingTypeCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService)
                .problems().stream().findFirst();

        assertThat(optionalProblem.get().getCode(), is(HEARING_TYPE_CODE_INVALID.name()));
        assertThat(optionalProblem.get().getValues().get(0).getKey(), is("hearingTypeCode"));
        assertThat(optionalProblem.get().getValues().get(0).getValue(), is(invalidPoliceForceCode));
    }

    @Test
    public void shouldReturnProblemWithEmptyStringValueWhenDefendantHearingTypeCodeIsNull() {

        final InitialHearing initialHearing = new InitialHearing(null,null,null,null,null,null,null, null, null, null, null);
        when(defendantWithReferenceData.getDefendant().getInitialHearing()).thenReturn(initialHearing);
        final ValidationResult optionalProblem = new HearingTypeCodeValidationRule().validate(defendantWithReferenceData, referenceDataQueryService);
        assertThat(optionalProblem.isValid(), is(true));
    }

    private HearingTypes getMockHearingTypesReferenceData(final String hearingCode, final String hearingDescription) {
        List<HearingType> hearingTypesReferenceData = new ArrayList<>();
        hearingTypesReferenceData.add(HearingType.hearingType()
                .withId(UUID.randomUUID())
                .withSeqId(20)
                .withHearingCode(HEARING_TYPE_CODE)
                .withHearingDescription(HEARING_DESCRIPTION)
                .build()

        );
        return HearingTypes.hearingTypes().withHearingtypes(hearingTypesReferenceData).build();
    }
}