package uk.gov.moj.cpp.prosecution.casefile.validation.rules;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.validation.ProblemCode.ENFORCEMENT_OFFENCE_NOT_FOUND;
import static uk.gov.moj.cpp.prosecution.casefile.validation.rules.FieldName.OFFENCE_CODE;

import uk.gov.moj.cpp.prosecution.casefile.domain.DefendantWithReferenceData;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.MojOffences;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.service.ReferenceDataQueryService;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnforcementOffencesValidationRuleTest {

    private static final String ALLOWED_CODE = "EF001";
    private static final String DISALLOWED_CODE = "XX999";

    @Mock
    ReferenceDataQueryService referenceDataQueryService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    DefendantWithReferenceData defendantWithReferenceData;

    @InjectMocks
    private EnforcementOffencesValidationRule underTest;

    @Nested
    @DisplayName("when submitted offence exists in active MOJ ref data")
    class AllowedOffence {
        @Test
        void validate_codeInAllowedSet_shouldReturnValid() {
            stubAllowed(ALLOWED_CODE);
            when(defendantWithReferenceData.getDefendant().getOffences())
                    .thenReturn(singletonList(offence(UUID.randomUUID(), ALLOWED_CODE)));

            assertThat(underTest.validate(defendantWithReferenceData, referenceDataQueryService), is(ValidationResult.VALID));
        }

        @Test
        void validate_multipleAllowedOffences_shouldReturnValid() {
            stubAllowed(ALLOWED_CODE, "EF002");
            when(defendantWithReferenceData.getDefendant().getOffences())
                    .thenReturn(asList(offence(UUID.randomUUID(), ALLOWED_CODE), offence(UUID.randomUUID(), "EF002")));

            assertThat(underTest.validate(defendantWithReferenceData, referenceDataQueryService), is(ValidationResult.VALID));
        }
    }

    @Nested
    @DisplayName("when submitted offence is not in active MOJ ref data")
    class DisallowedOffence {

        @Test
        void validate_codeNotInAllowedSet_shouldReturnNotFoundProblem() {
            UUID offenceId = UUID.randomUUID();
            stubAllowed(ALLOWED_CODE);
            when(defendantWithReferenceData.getDefendant().getOffences())
                    .thenReturn(singletonList(offence(offenceId, DISALLOWED_CODE)));

            List<Problem> problems = underTest.validate(defendantWithReferenceData, referenceDataQueryService).problems();

            assertThat(problems, hasSize(1));
            assertThat(problems.get(0).getCode(), is(ENFORCEMENT_OFFENCE_NOT_FOUND.name()));
            assertThat(problems.get(0).getValues().get(0).getValue(), is(DISALLOWED_CODE));
            assertThat(problems.get(0).getValues().get(0).getId(), is(offenceId.toString()));
            assertThat(problems.get(0).getValues().get(0).getKey(), is(OFFENCE_CODE.getValue()));
        }

        @Test
        void validate_noAllowedOffencesInRefData_shouldReturnNotFoundProblem() {
            when(referenceDataQueryService.retrieveAllActiveMojOffences()).thenReturn(emptyList());
            when(defendantWithReferenceData.getDefendant().getOffences())
                    .thenReturn(singletonList(offence(UUID.randomUUID(), ALLOWED_CODE)));

            List<Problem> problems = underTest.validate(defendantWithReferenceData, referenceDataQueryService).problems();

            assertThat(problems, hasSize(1));
            assertThat(problems.get(0).getCode(), is(ENFORCEMENT_OFFENCE_NOT_FOUND.name()));
        }

        @Test
        void validate_mixedOffences_reportsOnlyDisallowedOnes() {
            UUID disallowedId = UUID.randomUUID();
            stubAllowed(ALLOWED_CODE);
            when(defendantWithReferenceData.getDefendant().getOffences())
                    .thenReturn(asList(offence(UUID.randomUUID(), ALLOWED_CODE), offence(disallowedId, DISALLOWED_CODE)));

            List<Problem> problems = underTest.validate(defendantWithReferenceData, referenceDataQueryService).problems();

            assertThat(problems, hasSize(1));
            assertThat(problems.get(0).getValues(), hasSize(1));
            assertThat(problems.get(0).getValues().get(0).getValue(), is(DISALLOWED_CODE));
        }
    }

    private void stubAllowed(final String... codes) {
        when(referenceDataQueryService.retrieveAllActiveMojOffences())
                .thenReturn(asList(codes).stream()
                        .map(c -> MojOffences.mojOffences().withCjsOffenceCode(c).build())
                        .collect(java.util.stream.Collectors.toList()));
    }

    private static Offence offence(final UUID id, final String code) {
        return Offence.offence().withOffenceId(id).withOffenceCode(code).build();
    }
}
